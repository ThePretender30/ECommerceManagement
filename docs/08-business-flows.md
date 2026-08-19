# 08 — Business Flows

The important journeys, as sequence diagrams, with the rules that govern each.

---

## 1. Registration

```mermaid
sequenceDiagram
    participant U as User
    participant R as Register.jsx
    participant AC as AuthContext
    participant API as AuthController
    participant AS as AuthService
    participant DB as MySQL

    U->>R: fills the form
    R->>R: check password == confirmPassword<br/>(client-only; the API has no such field)
    R->>AC: register(payload)
    AC->>API: POST /api/auth/register
    API->>API: @Valid — email format, password ≥ 8,<br/>phone matches E.164
    API->>AS: register(request)
    AS->>DB: existsByEmail(email)?
    alt already registered
        AS-->>U: 409 "An account with this email already exists."
    else available
        AS->>AS: BCrypt.encode(password)
        AS->>DB: look up ROLE_CUSTOMER
        AS->>DB: INSERT user + user_roles
        AS->>AS: generate JWT
        AS-->>AC: { token, user }
        AC->>AC: store token + user, set state
        AC-->>U: signed in, redirected
    end
```

**Rules**

- Email is unique and lower-cased before storage.
- Password is BCrypt-hashed; the plain text never reaches the entity.
- Phone must be E.164 — it is the WhatsApp destination.
- **The role is assigned server-side and is always `ROLE_CUSTOMER`.** `RegisterRequest` has no
  `roles` field, so a request body cannot ask for admin.
- Success signs the user straight in, so registration is one step rather than two.

---

## 2. Sign in

```mermaid
sequenceDiagram
    participant U as User
    participant API as AuthController
    participant AS as AuthService
    participant AM as AuthenticationManager
    participant JS as JwtService

    U->>API: POST /api/auth/login
    API->>AS: login(request)
    AS->>AM: authenticate(email, password)
    AM->>AM: load user, BCrypt.matches(...)
    alt bad email OR bad password
        AM-->>U: 401 "Invalid email or password."
    else account disabled
        AM-->>U: 403 "This account has been disabled."
    else valid
        AM-->>AS: Authentication
        AS->>JS: generateToken(id, email, roles)
        AS-->>U: { token, user }
    end
```

**Rules**

- Delegating to `AuthenticationManager` means an unknown email and a wrong password produce the
  **same** exception and message — so the endpoint cannot be used to discover registered emails.
- Admins use this same endpoint; the frontend routes them to `/admin` afterwards based on their
  roles.
- Where the user is sent next: `?redirect=` if present, else `/admin` for admins, else `/`.

---

## 3. Add to cart

```mermaid
sequenceDiagram
    participant U as User
    participant CS as CartService
    participant PR as ProductRepository
    participant DB as MySQL

    U->>CS: addItem(userId, {productId, quantity})
    CS->>DB: find cart by userId
    alt no cart yet
        CS->>DB: INSERT cart
    end
    CS->>PR: find product
    alt product inactive
        CS-->>U: 400 "'X' is no longer available."
    end
    CS->>DB: existing cart_item for this product?
    Note over CS: newQuantity = existing ? existing + requested : requested
    CS->>CS: product.hasStockFor(newQuantity)?
    alt insufficient
        CS-->>U: 409 "Insufficient stock for 'X': requested 5, only 2 available."
    else ok
        CS->>DB: INSERT or UPDATE cart_item
        CS-->>U: full CartResponse with server-computed totals
    end
```

**Rules**

- The cart is resolved from the **authenticated user id only**. No endpoint accepts a cart id.
- Adding an existing product **increments** rather than duplicating — enforced by the unique
  constraint on `(cart_id, product_id)` as well as the service check.
- Quantity updates are **absolute**, not deltas, so a retried request is idempotent.
- No price is ever sent by the client; totals are computed server-side in `BigDecimal`.
- This stock check is a courtesy. The authoritative one happens at checkout.

---

## 4. Checkout

The most important flow in the system.

```mermaid
sequenceDiagram
    participant U as User
    participant OC as OrderController
    participant OS as OrderService
    participant AS as AddressService
    participant PR as ProductRepository
    participant DB as MySQL
    participant EV as Event publisher

    U->>OC: POST /api/orders { addressId }
    OC->>OS: placeOrder(userId, request)

    rect rgb(240, 245, 255)
    Note over OS,DB: ONE transaction — all of this, or none of it
    OS->>OS: exactly one address source supplied?
    OS->>DB: load the user's cart
    alt cart empty
        OS-->>U: 400 "Your cart is empty."
    end
    OS->>AS: findOwnedAddressOrThrow(userId, addressId)

    loop for every cart line
        OS->>PR: re-read the product IN this transaction
        OS->>OS: still active?
        OS->>OS: stock >= quantity?
        alt insufficient
            OS-->>U: 409 — ENTIRE transaction rolls back
        end
        OS->>OS: snapshot name + price onto an OrderItem
        OS->>DB: decrement product stock
    end

    OS->>DB: INSERT order (+ address snapshot, computed total)
    OS->>DB: INSERT order_items
    OS->>DB: INSERT order_status_history (ORDER_PLACED)
    OS->>DB: clear the cart
    end

    OS->>EV: publish OrderStatusChangedEvent
    Note right of EV: consumed AFTER COMMIT,<br/>on a background thread
    OS-->>U: 201 OrderResponse
```

### Why each guarantee holds

| Guarantee | Mechanism |
|---|---|
| **You cannot buy at a price you chose** | `PlaceOrderRequest` has no line items or prices. The order is rebuilt from the server-side cart, reading prices from the product rows. |
| **You cannot oversell** | Products are re-read *inside* the checkout transaction. A failure rolls back every decrement made so far. |
| **No half-written orders** | One `@Transactional` method covers validation, stock, order, items, history and cart clearing. |
| **Price changes cannot rewrite history** | `OrderItem` snapshots `productName` and `unitPrice`. |
| **Address edits cannot rewrite history** | `Order` snapshots all eight delivery fields. |
| **A Twilio outage cannot break checkout** | Notification is an event consumed after commit, asynchronously. |

> **Why stock is checked twice.** `CartService` checks when an item is added so the customer finds
> out early. But between adding to a cart and paying, another customer may take the last unit. Only
> the check inside the transaction that decrements stock is authoritative.

---

## 5. Order status lifecycle

```mermaid
stateDiagram-v2
    [*] --> ORDER_PLACED : checkout
    ORDER_PLACED --> ORDER_CONFIRMED : admin
    ORDER_PLACED --> CANCELLED : customer or admin
    ORDER_CONFIRMED --> PROCESSING : admin
    ORDER_CONFIRMED --> CANCELLED : customer or admin
    PROCESSING --> DISPATCHED : admin
    PROCESSING --> CANCELLED : customer or admin
    DISPATCHED --> OUT_FOR_DELIVERY : admin
    OUT_FOR_DELIVERY --> DELIVERED : admin
    DELIVERED --> [*]
    CANCELLED --> [*]
```

Every transition:

```mermaid
sequenceDiagram
    participant A as Admin
    participant OS as OrderService
    participant DB as MySQL
    participant L as OrderNotificationListener

    A->>OS: PUT /api/admin/orders/18/status { DISPATCHED }
    OS->>OS: current.canTransitionTo(DISPATCHED)?
    alt illegal
        OS-->>A: 400 "Cannot change status from 'Order Placed' to 'Delivered'.<br/>Allowed next steps: Cancelled, Order Confirmed."
    else legal
        opt target == CANCELLED
            OS->>DB: restore stock for every line
        end
        OS->>DB: UPDATE orders SET status
        OS->>DB: INSERT order_status_history
        OS->>OS: publish OrderStatusChangedEvent
        Note over OS,DB: transaction commits
        OS-->>A: 200 updated order
        DB->>L: AFTER_COMMIT → async
        L->>L: compose + send WhatsApp
        L->>DB: INSERT notification record
    end
```

**Rules**

- Legality is decided by `OrderStatus.canTransitionTo` — one map, consumed everywhere.
- `GET /api/admin/orders/statuses` exposes `allowedNext`, so the admin dropdown can only *offer*
  legal transitions. An illegal jump is unselectable, not merely rejected.
- Cancelling restores stock, whoever initiates it.
- Every change appends history; nothing is overwritten.
- `DELIVERED` and `CANCELLED` are terminal.

---

## 6. Cancellation

```mermaid
sequenceDiagram
    participant U as Customer
    participant OS as OrderService
    participant DB as MySQL

    U->>OS: PUT /api/orders/18/cancel
    OS->>DB: findByIdAndUserId(18, userId)
    alt not your order
        OS-->>U: 404
    end
    OS->>OS: status.canTransitionTo(CANCELLED)?
    alt already dispatched or beyond
        OS-->>U: 400 "This order can no longer be cancelled<br/>because it is already 'Dispatched'."
    else allowed
        loop every order item
            OS->>DB: product.stock += quantity
        end
        OS->>DB: status = CANCELLED, append history
        OS-->>U: 200 — WhatsApp cancellation sent
    end
```

**Why the cut-off is dispatch:** once a parcel has physically left, returning its items to the stock
count would make the inventory wrong. Those cases need a returns process, which this system does not
model.

---

## 7. Review eligibility

```mermaid
flowchart TD
    A[Customer opens a product page] --> B{Signed in?}
    B -->|No| C["Sign in to review products you have received."]
    B -->|Yes| D{"Has a DELIVERED order<br/>containing this product?"}
    D -->|No| E["You can review this product once an order<br/>containing it has been delivered."]
    D -->|Yes| F{Already reviewed it?}
    F -->|Yes| G[Show their review with Edit / Delete]
    F -->|No| H[Show 'Write a review']
    H --> I[POST /api/products/id/reviews]
    I --> J[Service re-checks eligibility]
    J --> K[Save review]
    K --> L[Recalculate averageRating<br/>and reviewCount on the product]
```

**Rules**

- Eligibility is `OrderRepository.hasUserPurchasedProduct` — a `DELIVERED` order containing that
  product. Placed-but-undelivered does not count.
- One review per customer per product, enforced by a unique constraint on
  `(product_id, user_id)`. Posting again updates the existing one.
- Rating is 1–5; text is optional.
- **The service re-checks eligibility on submit.** `canReview` in the summary response is for
  rendering; the authoritative check happens server-side on write.
- After any change, `products.average_rating` and `review_count` are recomputed from scratch, so
  they can never drift from the underlying rows.

---

## 8. Product browsing and filtering

```mermaid
sequenceDiagram
    participant U as User
    participant PL as ProductList.jsx
    participant URL as URL query string
    participant PC as ProductController
    participant PS as ProductService
    participant SP as ProductSpecification
    participant DB as MySQL

    U->>PL: types in the search box
    PL->>PL: useDebounce(450ms)
    PL->>URL: setParams({ q: 'kettle' })
    Note over URL: filters live in the URL,<br/>so results are shareable
    URL->>PL: re-render on change
    PL->>PC: GET /api/products?q=kettle&category=...&sort=price_asc
    PC->>PS: search(filter, page, size, includeInactive=false)
    PS->>PS: resolveSort() — allow-list, not raw input
    PS->>SP: withFilters(filter, false)
    SP->>SP: one predicate per supplied filter
    SP->>DB: single dynamic query + COUNT
    DB-->>U: PagedResponse
```

**Rules**

- One endpoint and one specification serve browse, category pages, search, filtering and sorting.
- Search covers name, description and brand.
- `sort` is an allow-list; unknown values fall back to `newest` rather than reaching the database.
- Only `active` products are returned to the storefront. The admin listing passes
  `includeInactive = true`.
- Search is debounced by 450 ms, so typing fires one request when you pause.
- Changing a filter resets to page 1 — otherwise narrowing a search while on page 5 lands you
  on an empty page.

---

## 9. Notification pipeline

```mermaid
sequenceDiagram
    participant OS as OrderService
    participant TX as Transaction
    participant EV as Spring events
    participant L as OrderNotificationListener
    participant W as WhatsAppService
    participant DB as MySQL

    OS->>EV: publishEvent(OrderStatusChangedEvent)
    Note over EV: held, not delivered yet
    OS->>TX: commit
    TX->>EV: committed
    EV->>L: @TransactionalEventListener(AFTER_COMMIT)<br/>+ @Async → background thread
    L->>L: compose message from templates
    L->>W: send(phone, message)
    alt Twilio configured
        W->>W: real API call
        W-->>L: SENT + message SID
    else not configured
        W->>W: log the message body
        W-->>L: SKIPPED
    end
    L->>DB: INSERT notification (SENT | FAILED | SKIPPED)
```

Covered in depth in [09 — WhatsApp Notifications](09-whatsapp-notifications.md). The essential
point: `AFTER_COMMIT` + `@Async` means a notification can never delay, block, or roll back an order.

---

**Next:** [09 — WhatsApp Notifications](09-whatsapp-notifications.md).
