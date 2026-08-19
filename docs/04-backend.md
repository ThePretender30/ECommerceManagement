# 04 — Backend

A tour of `backend/src/main/java/com/ecommerce/`, package by package. Read
[02 — Architecture](02-architecture.md) first if you have not.

---

## Package map

```
com.ecommerce
├─ EcommerceApplication.java     entry point; @EnableAsync for notifications
├─ config/                       cross-cutting configuration
├─ controller/                   REST endpoints
├─ service/                      business rules and transactions
├─ repository/                   Spring Data JPA + the product Specification
├─ entity/                       JPA entities and the order state machine
├─ dto/                          request/response records, grouped by area
├─ security/                     JWT, principal, filter, error handlers
├─ notification/                 WhatsApp abstraction, Twilio, fallback
├─ exception/                    custom exceptions + the global handler
└─ util/                         SlugUtils
```

---

## `entity` — the data model in Java

Eleven entities and three enums, mapping one-to-one with the tables in
[03 — Database](03-database.md).

Everything uses Lombok (`@Getter`, `@Setter`, `@Builder`) to keep the classes readable — the
interesting content is the annotations and the javadoc explaining *why* each mapping is the way it is.

### `OrderStatus` — the state machine

Worth reading on its own. The enum owns the transition rules rather than scattering `if` checks
through services:

```java
private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
        ORDER_PLACED,     EnumSet.of(ORDER_CONFIRMED, CANCELLED),
        ORDER_CONFIRMED,  EnumSet.of(PROCESSING, CANCELLED),
        PROCESSING,       EnumSet.of(DISPATCHED, CANCELLED),
        DISPATCHED,       EnumSet.of(OUT_FOR_DELIVERY),
        OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
        DELIVERED,        Collections.emptySet(),
        CANCELLED,        Collections.emptySet()
);

public boolean canTransitionTo(OrderStatus target) { ... }
public Set<OrderStatus> nextStatuses() { ... }
public boolean isCancellableByCustomer() { return canTransitionTo(CANCELLED); }
```

Three things consume this single definition:
- `OrderService.updateOrderStatus` rejects illegal transitions
- `OrderService.cancelOrder` uses `isCancellableByCustomer()`
- `GET /api/admin/orders/statuses` returns `nextStatuses()` so the admin UI can only *offer* legal
  options

Adding a status means editing one map.

### Fetch strategies

Mostly `LAZY`, with two deliberate exceptions:

| Relationship | Strategy | Why |
|---|---|---|
| `User.roles` | **EAGER** | Every authenticated request needs authorities to build the SecurityContext. Lazy would mean an extra query per request or a `LazyInitializationException` outside the transaction. |
| `CartItem.product` | **EAGER** | A cart line is meaningless without its product; you always render both together. |
| Everything else | LAZY | Loaded explicitly where needed, usually via `@EntityGraph`. |

Because `open-in-view` is `false`, a lazy association accessed outside its transaction fails
immediately in development rather than silently issuing N+1 queries in production.

---

## `repository` — data access

Twelve Spring Data JPA interfaces. Most are just derived query methods; three deserve attention.

### `ProductSpecification` — one query for everything

This is why the product API needs a single endpoint. Rather than
`findByCategoryAndBrandAndPriceBetweenAndRatingGreaterThan...` and its combinatorial cousins, each
supplied filter contributes one predicate:

```java
if (q != null) {
    predicates.add(cb.or(
        cb.like(cb.lower(root.get("name")), pattern),
        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
        cb.like(cb.lower(cb.coalesce(root.get("brand"), "")), pattern)));
}
if (filter.categoryId() != null) { ... }
if (filter.minPrice() != null)   { ... }
```

Browsing everything, filtering Electronics under ₹5000 with a 4-star minimum, and searching for
"kettle" are all the same query with different predicate sets.

Two details in there worth noting:

- **Every value is a bound Criteria parameter**, never string-concatenated into SQL. These filters
  cannot be used for injection.
- The category join fetch is guarded:
  ```java
  if (query != null && query.getResultType() != Long.class) {
      root.fetch("category", JoinType.LEFT);
      query.distinct(true);
  }
  ```
  Spring Data reuses the same specification for the `COUNT` query that powers pagination, and a
  join fetch is illegal there. Without this guard, every paged product request fails.

### `OrderRepository` — aggregates and the review rule

Holds the dashboard aggregates as real SQL (`SUM`, `GROUP BY`) rather than loading rows and counting
in Java, plus the review-eligibility check:

```java
@Query("""
    SELECT COUNT(oi) > 0 FROM OrderItem oi
    WHERE oi.order.user.id = :userId
      AND oi.product.id    = :productId
      AND oi.order.status  = com.ecommerce.entity.OrderStatus.DELIVERED
    """)
boolean hasUserPurchasedProduct(Long userId, Long productId);
```

Statistics use **Spring Data projections** (`StatusCount`, `TopProduct`) — interfaces the framework
implements, so read-only aggregates need no DTO class.

### `CartRepository` — the shape of a security guarantee

```java
@EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
Optional<Cart> findByUserId(Long userId);
```

There is deliberately **no** "find cart by id" method exposed to controllers. The cart is always
derived from the authenticated user, so cross-user access is not something a future endpoint could
forget to check — the lookup does not exist. The entity graph makes rendering a cart one query
rather than N+1.

---

## `service` — the business rules

Where all the actual logic lives. Each service is `@Transactional` at the method level, with
`readOnly = true` on queries.

| Service | Responsibility |
|---|---|
| `AuthService` | Registration, login, current user |
| `UserService` | Profile self-service; admin user list and enable/disable |
| `CategoryService` | Category browsing and CRUD |
| `ProductService` | Browsing, search, admin CRUD, stock, soft delete |
| `CartService` | The cart |
| `AddressService` | The address book |
| `OrderService` | Checkout and the order lifecycle |
| `ReviewService` | Reviews and rating recalculation |
| `AdminStatsService` | Dashboard aggregates |
| `NotificationService` | Read access to the notification audit log |

### `OrderService.placeOrder` — the most important method

Checkout is one `@Transactional` method. Reading it end to end explains most of the system's
correctness guarantees:

1. **Resolve the address.** Either a saved one (ownership-checked) or a new one from the request.
2. **Loop the cart lines.** For each:
   - Re-read the product *inside this transaction* to get its committed stock
   - Reject if it has been deactivated
   - Reject if stock is insufficient → `InsufficientStockException` rolls back everything
   - **Snapshot** name, image and price onto a new `OrderItem`
   - Decrement stock
3. **Set the total** from the accumulated line totals.
4. **Write the first status history row** (`ORDER_PLACED`).
5. **Clear the cart.**
6. **Publish** an `OrderStatusChangedEvent`.

Two points that are easy to miss:

> **Stock is validated twice on purpose.** `CartService` checks when an item is added, so the
> customer finds out early. `OrderService` checks again at checkout, because between those two
> moments another customer may have bought the last unit. Only the second check is authoritative,
> because only it runs inside the transaction that decrements stock.

> **The request carries no line items.** `PlaceOrderRequest` has only an address choice. The order
> is rebuilt from the server-side cart, which is what makes it impossible to check out at a price
> or quantity the client invented.

### `OrderService.updateOrderStatus`

Validates against `OrderStatus.canTransitionTo` before writing anything, restores stock if the
target is `CANCELLED`, appends a history row, and publishes an event. The rejection message names
the legal next steps:

```
Cannot change status from 'Order Placed' to 'Delivered'.
Allowed next steps: Cancelled, Order Confirmed.
```

### Why events instead of a direct call

`OrderService` never calls the WhatsApp sender. It publishes an event; `OrderNotificationListener`
consumes it after commit, asynchronously. Reasoning in [09](09-whatsapp-notifications.md), but the
short version: a notification must never be able to delay or roll back an order.

---

## `dto` — the API's public shape

Java **records**, grouped into sub-packages (`auth`, `product`, `cart`, `order`, `address`,
`review`, `admin`, `category`, `common`).

Requests carry `jakarta.validation` annotations:

```java
public record AddToCartRequest(
        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100 per item")
        Integer quantity
) {}
```

Responses expose static factories that map from an entity — `ProductResponse.from(product)`,
`OrderResponse.forAdmin(order)`.

**What is absent from a DTO matters as much as what is present:**

| DTO | Deliberately has no… | So that… |
|---|---|---|
| `RegisterRequest` | `roles` | A sign-up cannot make itself an admin |
| `ProductRequest` | `averageRating`, `reviewCount` | An admin cannot fabricate ratings |
| `AddToCartRequest` | `price` | A request cannot set what it pays |
| `PlaceOrderRequest` | line items | The order comes from the server-side cart |
| `UserResponse` | `password` | The hash cannot leak |

### `PagedResponse<T>`

Spring's `Page` serialises to a large, version-sensitive structure. `PagedResponse` flattens it to
a small stable envelope, so the frontend's pagination depends on our contract rather than Spring
Data internals:

```json
{ "content": [...], "page": 0, "size": 12, "totalElements": 42,
  "totalPages": 4, "first": true, "last": false, "empty": false }
```

### `OrderTrackingResponse` — logic in the DTO, on purpose

This one contains real logic: it converts an order's status and history into a list of
`TrackingStep`s already marked `completed` / `current` / `pending`, with the timestamp each stage
was reached.

It lives here so the UI never has to hard-code the delivery sequence. A cancelled order gets its own
honest two-step timeline instead of a progress bar frozen halfway — and because the backend decides,
the customer and admin views cannot disagree.

---

## `controller` — thin by design

Eight controllers. Every method follows the same shape: validate, delegate, map, choose a status
code. There is **no database access and no `try/catch` anywhere in this package**.

| Controller | Base path | Auth |
|---|---|---|
| `AuthController` | `/api/auth` | Mixed |
| `ProductController` | `/api/products` | Public reads; reviews need auth |
| `CategoryController` | `/api/categories` | Public |
| `CartController` | `/api/cart` | Authenticated |
| `OrderController` | `/api/orders` | Authenticated |
| `AddressController` | `/api/addresses` | Authenticated |
| `AdminProductController` | `/api/admin/products` | `ROLE_ADMIN` |
| `AdminCategoryController` | `/api/admin/categories` | `ROLE_ADMIN` |
| `AdminOrderController` | `/api/admin/orders` | `ROLE_ADMIN` |
| `AdminDashboardController` | `/api/admin` | `ROLE_ADMIN` |

Note there are **no `@PreAuthorize` annotations on the admin controllers**. A single rule in
`SecurityConfig` covers `/api/admin/**`, which means a newly added admin endpoint is protected by
default rather than depending on the author remembering an annotation.

### How identity reaches the service

```java
@GetMapping
public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(cartService.getCart(principal.getId()));
}
```

`@AuthenticationPrincipal` supplies the user from the *verified token*. Services take that id. No
endpoint accepts a user id from the request body or path — which is why there is no request a
customer could craft to read another's cart or orders.

`ProductController.reviewSummary` is the one place the principal is optional — anonymous visitors get
the ratings, signed-in ones additionally get their eligibility and their own review.

---

## `exception` — one error shape

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps every exception to `ApiError`:

```json
{
  "timestamp": "2026-08-17T09:14:22Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient stock for 'Cast Iron Skillet': requested 3, only 1 available.",
  "path": "/api/cart/items",
  "fieldErrors": null
}
```

| Exception | Status | Meaning |
|---|---|---|
| `ResourceNotFoundException` | 404 | Does not exist, or is not visible to you |
| `BadRequestException` | 400 | Valid shape, broken business rule |
| `InsufficientStockException` | **409** | The request was fine; the world changed |
| `DuplicateResourceException` | 409 | Uniqueness violation |
| `MethodArgumentNotValidException` | 400 | Bean validation → populates `fieldErrors` |
| `BadCredentialsException` | 401 | Deliberately vague: "Invalid email or password" |
| `AccessDeniedException` | 403 | Authenticated but not permitted |
| `DataIntegrityViolationException` | 409 | A constraint the service check missed |
| `Exception` | 500 | Logged server-side; generic message to the client |

**Why 409 for stock:** 400 says "your request was malformed", which is untrue — the customer asked
for something that was available when they added it. 409 Conflict says "the current state of the
resource prevents this", which is what actually happened.

**Why login errors are vague:** the same message for an unknown email and a wrong password means
the endpoint cannot be used to discover which emails are registered.

The catch-all handler logs the real exception and returns a generic message, so a stack trace or
SQL fragment can never reach a client.

---

## `config`

| Class | Purpose |
|---|---|
| `SecurityConfig` | The filter chain and authorisation rules — see [05](05-security.md) |
| `JwtProperties` | Binds `app.jwt.*`; **fails startup** if the secret is missing or under 32 chars |
| `CorsProperties` | Binds `app.cors.allowed-origins` |
| `AdminSeedProperties` | Binds `app.admin.*` for the bootstrap account |
| `WhatsAppProperties` | Binds `app.whatsapp.*`; `isFullyConfigured()` picks the sender |
| `NotificationConfig` | Chooses Twilio or the logging fallback, once, at startup |
| `DataSeeder` | Seeds roles, admin, six categories, ~42 products |

### Fail-fast configuration

`JwtProperties.validate()` runs at startup:

```
============================================================
 APP_JWT_SECRET is not set.

 Create backend/.env (copy from .env.example) and set:
   APP_JWT_SECRET=<at least 32 random characters>

 Then start the app with:  .\run.ps1
============================================================
```

Refusing to boot is deliberate. A hard-coded fallback secret would work perfectly in development and
be a serious vulnerability in production — exactly the kind of thing that never gets removed.

`DataSeeder` applies the same reasoning to `ADMIN_PASSWORD`: if it is absent, no admin is created
and a warning is logged. No default admin password exists to be forgotten about.

### `DataSeeder` is idempotent

Every step checks before inserting. Restarting never duplicates rows, and products are seeded only
when the table is empty — so a product an admin deliberately deleted is not resurrected on the next
restart.

The seeded products are ordinary database rows: editable, deletable, orderable. They are starting
inventory, not frontend placeholders.

---

## `util`

`SlugUtils.toSlug` converts a display name to a URL-safe slug, stripping accents via Unicode
normalisation so "Café" becomes `cafe` rather than losing the character.

---

**Next:** [05 — Security](05-security.md) for the authentication and authorisation model.
