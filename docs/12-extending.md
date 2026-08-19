# 12 — Extending

Recipes for common changes, each following the patterns already in the codebase. Copy the shape of
what exists rather than inventing a new one.

---

## Recipe 1 — Add a new entity, end to end

Worked example: a **Wishlist**, where a customer saves products to buy later.

### 1. Entity — `entity/WishlistItem.java`

```java
@Entity
@Table(name = "wishlist_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"}),
        indexes = @Index(name = "idx_wishlist_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WishlistItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wishlist_user"))
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wishlist_product"))
    private Product product;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    void onCreate() { if (addedAt == null) addedAt = Instant.now(); }
}
```

Conventions being followed: named foreign keys and constraints, `LAZY` by default with `EAGER` only
where the association is always rendered, `@PrePersist` for timestamps, a unique constraint
expressing the business rule ("one entry per product per user") at the database level.

### 2. Repository — `repository/WishlistItemRepository.java`

```java
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    // Scoped by user id — the pattern that makes cross-user access impossible.
    List<WishlistItem> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
```

> **Do not add `findById` to the controller's reach.** Follow `CartRepository`: expose only
> user-scoped lookups, so a future endpoint cannot accidentally leak another user's data.

### 3. DTO — `dto/wishlist/WishlistItemResponse.java`

```java
public record WishlistItemResponse(
        Long id, Long productId, String productName, String productImageUrl,
        BigDecimal price, boolean inStock, Instant addedAt
) {
    public static WishlistItemResponse from(WishlistItem item) {
        var p = item.getProduct();
        return new WishlistItemResponse(
                item.getId(), p.getId(), p.getName(), p.getImageUrl(),
                p.getPrice(), p.isInStock(), item.getAddedAt());
    }
}
```

### 4. Service — `service/WishlistService.java`

```java
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductService productService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId)
                .stream().map(WishlistItemResponse::from).toList();
    }

    @Transactional
    public WishlistItemResponse add(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("That product is already in your wishlist.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Product product = productService.findProductOrThrow(productId);

        return WishlistItemResponse.from(wishlistRepository.save(
                WishlistItem.builder().user(user).product(product).build()));
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("That product is not in your wishlist.");
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
```

Every method takes `userId` as its first parameter. That is the convention that keeps authorisation
structural rather than something to remember.

### 5. Controller — `controller/WishlistController.java`

```java
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Save products for later")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(wishlistService.getWishlist(principal.getId()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistItemResponse> add(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.add(principal.getId(), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        wishlistService.remove(principal.getId(), productId);
        return ResponseEntity.noContent().build();
    }
}
```

### 6. Security

**Nothing to do.** `/api/wishlist` matches no public rule, so `anyRequest().authenticated()`
protects it automatically. That default is why forgetting to configure a new endpoint locks it down
rather than exposing it.

### 7. Frontend service — `services/wishlistService.js`

```js
import api from './api'

export const wishlistService = {
  list: () => api.get('/wishlist').then((r) => r.data),
  add: (productId) => api.post(`/wishlist/${productId}`).then((r) => r.data),
  remove: (productId) => api.delete(`/wishlist/${productId}`).then((r) => r.data),
}
```

### 8. Page and route

Create `pages/Wishlist.jsx` following the load/error/empty/content pattern, then register it:

```jsx
<Route path="/wishlist" element={<ProtectedRoute><Wishlist /></ProtectedRoute>} />
```

### 9. Schema documentation

Add the table to `database/schema.sql`. Hibernate creates it automatically, but that file is the
readable reference and should stay in step.

---

## Recipe 2 — Add a field to an existing entity

Example: a `sku` on `Product`.

1. **Entity** — add the field with its column mapping:
   ```java
   @Column(length = 50, unique = true)
   private String sku;
   ```
2. **Request DTO** — add it to `ProductRequest` with validation. *Only if an admin should set it.*
   Derived or system-owned values must stay out of request DTOs.
3. **Response DTO** — add it to `ProductResponse` and its `from(...)` mapper.
4. **Service** — set it in `create` and `update`.
5. **Frontend** — add the input to `AdminProductForm`, and display it where useful.
6. **`schema.sql`** — add the column.

Hibernate's `ddl-auto: update` adds the column on restart. Note that it **adds but never drops** —
removing a field leaves the column behind, which you must drop manually.

---

## Recipe 3 — Add an order status

Example: `RETURNED`, after `DELIVERED`.

**One file does most of the work** — `entity/OrderStatus.java`:

```java
RETURNED("Returned"),
```

then update the transition map:

```java
DELIVERED, EnumSet.of(RETURNED),      // was Collections.emptySet()
RETURNED,  Collections.emptySet(),
```

Then:

1. **Message template** — add a `case RETURNED ->` to `WhatsAppMessageTemplates.forStatus`. The
   `switch` is exhaustive over the enum, so **the compiler will not let you forget this**.
2. **Business logic** — if returning should restore stock, extend the `if (target == CANCELLED)`
   branch in `OrderService.updateOrderStatus`.
3. **Tracking timeline** — decide whether `RETURNED` belongs in the happy path in
   `OrderTrackingResponse.HAPPY_PATH`, or gets special handling like `CANCELLED`.
4. **Frontend badge colour** — add a case to `statusBadgeClass` in `utils/format.js`.
5. **Admin filter** — add it to `STATUS_FILTERS` in `AdminOrders.jsx`.

The admin status dropdown needs no change: it is built from `GET /api/admin/orders/statuses`, which
reads the enum.

---

## Recipe 4 — Add an API endpoint to an existing resource

Example: "reorder" — copy a past order's items back into the cart.

**Service first** (`OrderService`):

```java
@Transactional
public CartResponse reorder(Long userId, Long orderId) {
    Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

    for (OrderItem item : order.getItems()) {
        Product product = item.getProduct();
        // Skip anything deleted or deactivated since — do not fail the whole reorder.
        if (product != null && product.isActive() && product.hasStockFor(item.getQuantity())) {
            cartService.addItem(userId, new AddToCartRequest(product.getId(), item.getQuantity()));
        }
    }
    return cartService.getCart(userId);
}
```

**Then the controller** (`OrderController`):

```java
@PostMapping("/{id}/reorder")
@Operation(summary = "Add this order's items back into your cart")
public ResponseEntity<CartResponse> reorder(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id) {
    return ResponseEntity.ok(orderService.reorder(principal.getId(), id));
}
```

**Then the frontend** — add to `orderService.js`, then wire a button on `OrderTracking`.

Order matters: service → controller → frontend. Writing the controller first tempts you to put logic
in it.

---

## Recipe 5 — Add a notification channel

Example: email alongside WhatsApp.

The seam already exists. `OrderService` publishes events and knows nothing about delivery.

1. **Implement the interface:**
   ```java
   public class EmailNotificationService implements WhatsAppService {
       @Override public SendResult send(String recipient, String message) { ... }
       @Override public String providerName() { return "EMAIL"; }
   }
   ```
   If you want *both* channels rather than one or the other, first rename the interface to
   `NotificationChannel` and have the listener iterate a `List<NotificationChannel>` that Spring
   injects.

2. **Register it** in `NotificationConfig`.

3. **Templates** — add an `EmailMessageTemplates` alongside the WhatsApp one.

4. **Persistence** — nothing to change. The `notifications` table already has a `channel` column.

---

## Recipe 6 — Add a product filter

Example: filter by "on sale".

1. **Entity** — add the field (e.g. `discountPercent`).
2. **`ProductFilterRequest`** — add the parameter.
3. **`ProductSpecification`** — add one predicate:
   ```java
   if (Boolean.TRUE.equals(filter.onSale())) {
       predicates.add(cb.greaterThan(root.get("discountPercent"), 0));
   }
   ```
4. **`ProductController`** — add the `@RequestParam` and pass it through.
5. **Frontend** — add the control to `ProductList.jsx`'s filter sidebar, reading and writing through
   `useQueryParams` so it participates in shareable URLs.

Adding a *sort* option instead? Add a case to `ProductService.resolveSort` — and keep it an
allow-list; never pass raw input through as a property name.

---

## Conventions to follow

| Area | Convention |
|---|---|
| **Entities** | Named FK/unique constraints; `LAZY` by default; `@PrePersist` for timestamps; javadoc explaining *why*, not what |
| **Repositories** | User-scoped lookups (`findByIdAndUserId`) for anything owned by a customer |
| **Services** | `userId` first parameter; `@Transactional(readOnly = true)` on queries; throw domain exceptions, never return null |
| **Controllers** | Thin. No database access, no `try/catch`, no business rules |
| **DTOs** | Records. Requests carry validation; omit fields the client must not control |
| **Errors** | Throw a domain exception; `GlobalExceptionHandler` does the rest |
| **Money** | `BigDecimal` always. Construct from strings, never doubles |
| **Frontend state** | Server response replaces local state; never patch a local copy |
| **Frontend filters** | In the URL via `useQueryParams`, not component state |
| **CSS** | Tokens from `tokens.css`; component styles beside the component; mobile-first |

---

## Before you commit

```powershell
# Backend compiles
cd backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd -q compile

# Frontend builds and lints
cd ..\frontend
npm run build
npm run lint

# End-to-end behaviour still works
cd ..\backend
.\smoke-test.ps1 -AdminPassword '<your password>'
```

If you added an endpoint, extend `smoke-test.ps1` with a `Test-Step` for it — and add a row to
[06 — API Reference](06-api-reference.md).

---

## Where to look for a pattern

| You are adding… | Copy the shape of… |
|---|---|
| A user-owned resource | `AddressService` + `AddressController` |
| A paginated admin list | `AdminProducts.jsx` + `AdminProductController` |
| A create/edit form | `AdminProductForm.jsx` |
| A dynamic filter | `ProductSpecification` |
| A dashboard statistic | `AdminStatsService` |
| A rule spanning several tables | `ReviewService` eligibility check |
| A side effect that must not block | `OrderNotificationListener` |
| A modal workflow | `Addresses.jsx` |

---

**Back to:** [README](../README.md) · [00 — Overview](00-overview.md)
