# 06 — API Reference

Every endpoint, with its authorisation requirement, request shape, response shape and status codes.

An interactive version is available while the backend is running:
**http://localhost:8080/swagger-ui.html**

---

## Conventions

**Base URL:** `http://localhost:8080/api`

**Authentication:** send the JWT on every protected request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Auth column:**

| Symbol | Meaning |
|---|---|
| — | Public; no token needed |
| 🔒 | Any authenticated user |
| 🛡️ | `ROLE_ADMIN` only |

**Every error** returns the same shape:

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

`fieldErrors` is populated only for validation failures:

```json
{
  "status": 400,
  "message": "Validation failed. Please check the highlighted fields.",
  "fieldErrors": {
    "email": "Please enter a valid email address",
    "phoneNumber": "Phone number must be in international format, e.g. +919876543210"
  }
}
```

**Paginated responses** always use this envelope:

```json
{
  "content": [ ... ],
  "page": 0, "size": 12,
  "totalElements": 42, "totalPages": 4,
  "first": true, "last": false, "empty": false
}
```

---

## Authentication — `/api/auth`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/register` | — | Create a customer account |
| POST | `/login` | — | Sign in |
| GET | `/me` | 🔒 | Current user's profile |
| PUT | `/me` | 🔒 | Update name and phone |
| PUT | `/me/password` | 🔒 | Change password |
| POST | `/logout` | 🔒 | Acknowledgement (client discards the token) |

### `POST /api/auth/register` → `201`

```json
{
  "fullName": "Aisha Sharma",
  "email": "aisha@example.com",
  "password": "SecurePass123",
  "phoneNumber": "+919876543210"
}
```

There is no `roles` field. New accounts always receive `ROLE_CUSTOMER`.

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": 7, "fullName": "Aisha Sharma", "email": "aisha@example.com",
    "phoneNumber": "+919876543210", "roles": ["ROLE_CUSTOMER"],
    "enabled": true, "createdAt": "2026-08-17T09:00:00Z"
  }
}
```

**Errors:** `400` validation · `409` email already registered

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Aisha Sharma","email":"aisha@example.com","password":"SecurePass123","phoneNumber":"+919876543210"}'
```

### `POST /api/auth/login` → `200`

```json
{ "email": "aisha@example.com", "password": "SecurePass123" }
```

Same `AuthResponse` as registration. **Errors:** `401` invalid credentials (deliberately vague) ·
`403` account disabled

### `PUT /api/auth/me` → `200`

```json
{ "fullName": "Aisha Sharma", "phoneNumber": "+919876543211" }
```

Email is not editable — it is the sign-in identity and the JWT subject.

### `PUT /api/auth/me/password` → `200`

```json
{ "currentPassword": "SecurePass123", "newPassword": "EvenBetter456" }
```

**Errors:** `400` current password wrong or new one too short · `409` new password same as current

---

## Categories — `/api/categories`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/` | — | All categories with product counts |
| GET | `/slug/{slug}` | — | One category by URL slug |
| GET | `/{id}` | — | One category by id |

```json
[
  { "id": 3, "name": "Kitchen Utensils", "slug": "kitchen-utensils",
    "description": "Cookware, tools and gadgets for every kitchen.",
    "imageUrl": "https://...", "productCount": 7 }
]
```

---

## Products — `/api/products`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/` | — | Browse / search / filter / sort |
| GET | `/{id}` | — | Product detail |
| GET | `/featured` | — | Top-rated (home page) |
| GET | `/popular` | — | Most reviewed |
| GET | `/new-arrivals` | — | Most recently added |
| GET | `/brands` | — | Distinct brands for filters |
| GET | `/{id}/reviews` | — | Paginated reviews |
| GET | `/{id}/reviews/summary` | — * | Rating summary + your eligibility |
| POST | `/{id}/reviews` | 🔒 | Write or update your review |
| DELETE | `/{id}/reviews/mine` | 🔒 | Delete your review |

\* Public, but returns extra fields when a token is supplied.

### `GET /api/products`

The single endpoint behind browsing, category pages, search and filtering. All parameters optional.

| Parameter | Type | Notes |
|---|---|---|
| `q` | string | Searches name, description and brand |
| `category` | string | Category **slug**, e.g. `electronics` |
| `categoryId` | number | Takes precedence over `category` |
| `brand` | string | Exact match |
| `minPrice` / `maxPrice` | number | Inclusive |
| `minRating` | number | Inclusive lower bound |
| `inStock` | boolean | `true` hides zero-stock products |
| `sort` | string | See below |
| `page` | number | 0-based, default `0` |
| `size` | number | Default `12`, max `100` |

**`sort` values:** `newest` (default) · `oldest` · `price_asc` · `price_desc` · `rating` ·
`popular` · `name_asc` · `name_desc`. Unrecognised values fall back to `newest`.

```bash
# Electronics under ₹15000, rated 4+, in stock, cheapest first
curl "http://localhost:8080/api/products?category=electronics&maxPrice=15000&minRating=4&inStock=true&sort=price_asc"
```

**Response:** a `PagedResponse` of:

```json
{
  "id": 31, "name": "Wireless Noise Cancelling Headphones",
  "description": "Over-ear ANC headphones with 30-hour battery life...",
  "price": 12999.00,
  "categoryId": 5, "categoryName": "Electronics", "categorySlug": "electronics",
  "brand": "AudioPeak", "imageUrl": "https://...",
  "stock": 25, "inStock": true,
  "averageRating": 4.50, "reviewCount": 2,
  "dateAdded": "2026-08-17T08:00:00Z", "active": true
}
```

Only `active` products appear here. `GET /{id}` returns `404` for a deactivated product.

### `GET /api/products/{id}/reviews/summary`

```json
{
  "productId": 31,
  "averageRating": 4.50,
  "totalReviews": 2,
  "ratingBreakdown": { "5": 1, "4": 1, "3": 0, "2": 0, "1": 0 },
  "canReview": true,
  "userReview": null
}
```

`canReview` is `true` only when the caller has a **`DELIVERED` order containing this product** and
has not already reviewed it. `false` for anonymous callers.

### `POST /api/products/{id}/reviews` → `201`

```json
{ "rating": 4, "reviewText": "Solid product, arrived quickly." }
```

`rating` is 1–5 and required; `reviewText` is optional. Posting again updates your existing review.

**Errors:** `400` you have no delivered order containing this product · `404` no such product

---

## Cart — `/api/cart` 🔒

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Your cart (created empty on first access) |
| POST | `/items` | Add, or increment if already present |
| PUT | `/items/{itemId}` | Set an absolute quantity |
| DELETE | `/items/{itemId}` | Remove one line |
| DELETE | `/` | Empty the cart |

**No route contains a cart id.** The cart is always resolved from the authenticated user.

### `POST /api/cart/items` → `200`

```json
{ "productId": 31, "quantity": 2 }
```

No price field — the server reads the current price from the product row.

**Response** (returned by every cart operation):

```json
{
  "id": 4,
  "items": [
    { "id": 12, "productId": 31, "productName": "Wireless Noise Cancelling Headphones",
      "productImageUrl": "https://...", "categoryName": "Electronics",
      "unitPrice": 12999.00, "quantity": 2, "lineTotal": 25998.00,
      "availableStock": 25, "stockSufficient": true }
  ],
  "totalItems": 1, "totalQuantity": 2,
  "subtotal": 25998.00, "total": 25998.00,
  "checkoutAllowed": true
}
```

`stockSufficient` per line and `checkoutAllowed` overall let the cart page flag an item that sold
out after it was added, before the customer reaches checkout.

**Errors:** `400` product inactive · `404` no such product · **`409`** insufficient stock

### `PUT /api/cart/items/{itemId}` → `200`

```json
{ "quantity": 3 }
```

Absolute, not a delta — so a retried request is idempotent. Minimum `1`; remove the item instead of
setting `0`.

**Errors:** `404` item not in *your* cart · `409` insufficient stock

---

## Addresses — `/api/addresses` 🔒

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Your addresses, default first |
| GET | `/{id}` | One address |
| POST | `/` | Save a new address |
| PUT | `/{id}` | Update |
| PUT | `/{id}/default` | Make this the default |
| DELETE | `/{id}` | Delete |

### `POST /api/addresses` → `201`

```json
{
  "fullName": "Aisha Sharma",
  "phone": "+919876543210",
  "line1": "42 Marine Drive",
  "line2": "Near Chowpatty",
  "city": "Mumbai",
  "state": "Maharashtra",
  "postalCode": "400002",
  "country": "India",
  "isDefault": true
}
```

The first address saved becomes the default automatically. Setting a new default demotes the
previous one.

Deleting is always safe — orders keep their own snapshot of the delivery address.

---

## Orders — `/api/orders` 🔒

| Method | Path | Purpose |
|---|---|---|
| POST | `/` | Place an order from your cart |
| GET | `/` | Your order history (paginated) |
| GET | `/{id}` | One order in full |
| GET | `/{id}/tracking` | Status timeline |
| PUT | `/{id}/cancel` | Cancel (before dispatch only) |

### `POST /api/orders` → `201`

Supply **exactly one** of `addressId` or `newAddress`:

```json
{ "addressId": 5 }
```

```json
{
  "newAddress": {
    "fullName": "Aisha Sharma", "phone": "+919876543210",
    "line1": "42 Marine Drive", "city": "Mumbai",
    "state": "Maharashtra", "postalCode": "400002", "country": "India"
  },
  "saveNewAddress": true
}
```

**There are no line items in this request.** The order is built from your server-side cart.

**Response:**

```json
{
  "id": 18, "orderNumber": "ORD-20260817-4821",
  "status": "ORDER_PLACED", "statusLabel": "Order Placed",
  "totalAmount": 25998.00,
  "items": [
    { "id": 33, "productId": 31,
      "productName": "Wireless Noise Cancelling Headphones",
      "unitPrice": 12999.00, "quantity": 2, "lineTotal": 25998.00,
      "productAvailable": true }
  ],
  "totalQuantity": 2,
  "deliveryFullName": "Aisha Sharma",
  "deliveryPhone": "+919876543210",
  "deliveryAddress": "42 Marine Drive, Mumbai, Maharashtra 400002, India",
  "placedAt": "2026-08-17T09:20:00Z",
  "cancellable": true,
  "statusHistory": [
    { "id": 40, "status": "ORDER_PLACED", "statusLabel": "Order Placed",
      "note": "Order placed by customer.", "changedAt": "2026-08-17T09:20:00Z" }
  ]
}
```

Side effects: stock decremented, cart emptied, first history row written, WhatsApp notification
dispatched after commit.

**Errors:** `400` empty cart, or both/neither address option · `404` address not yours ·
**`409`** insufficient stock (the entire order rolls back)

### `GET /api/orders/{id}/tracking` → `200`

```json
{
  "orderId": 18, "orderNumber": "ORD-20260817-4821",
  "currentStatus": "DISPATCHED", "currentStatusLabel": "Dispatched",
  "placedAt": "2026-08-17T09:20:00Z",
  "cancelled": false, "delivered": false,
  "progressSteps": [
    { "status": "ORDER_PLACED",     "label": "Order Placed",     "state": "completed", "occurredAt": "2026-08-17T09:20:00Z" },
    { "status": "ORDER_CONFIRMED",  "label": "Order Confirmed",  "state": "completed", "occurredAt": "2026-08-17T10:05:00Z" },
    { "status": "PROCESSING",       "label": "Processing",       "state": "completed", "occurredAt": "2026-08-17T11:30:00Z" },
    { "status": "DISPATCHED",       "label": "Dispatched",       "state": "current",   "occurredAt": "2026-08-17T14:00:00Z" },
    { "status": "OUT_FOR_DELIVERY", "label": "Out for Delivery", "state": "pending",   "occurredAt": null },
    { "status": "DELIVERED",        "label": "Delivered",        "state": "pending",   "occurredAt": null }
  ],
  "history": [ ... ]
}
```

The backend decides each step's `state`, so the UI never hard-codes the delivery sequence. A
cancelled order returns a two-step timeline instead.

### `PUT /api/orders/{id}/cancel` → `200`

Optional `?reason=Changed my mind`. Restores stock and notifies the customer.

**Errors:** `400` already dispatched or beyond · `404` not your order

---

## Admin — Products 🛡️ `/api/admin/products`

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | All products, **including deactivated** |
| GET | `/{id}` | One product, active or not |
| POST | `/` | Create |
| PUT | `/{id}` | Update |
| PATCH | `/{id}/stock` | Set stock level |
| DELETE | `/{id}` | Delete, or soft-delete if ordered |

### `POST /api/admin/products` → `201`

```json
{
  "name": "Ceramic Non-Stick Frying Pan 28cm",
  "description": "PFOA-free ceramic coating, induction compatible.",
  "price": 1899.00,
  "categoryId": 3,
  "brand": "ChefLine",
  "imageUrl": "https://example.com/pan.jpg",
  "stock": 30,
  "active": true
}
```

No `averageRating` or `reviewCount` — those are derived from real reviews.

### `PATCH /api/admin/products/{id}/stock` → `200`

```json
{ "stock": 45 }
```

### `DELETE /api/admin/products/{id}` → `200`

Returns which action was taken:

```json
{ "message": "This product appears in existing orders, so it was deactivated and hidden from the store rather than deleted." }
```

or

```json
{ "message": "Product deleted." }
```

---

## Admin — Categories 🛡️ `/api/admin/categories`

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | List with product counts |
| POST | `/` | Create |
| PUT | `/{id}` | Update |
| DELETE | `/{id}` | Delete (empty categories only) |

```json
{ "name": "Sports Equipment", "slug": "sports-equipment",
  "description": "Gear for every sport.", "imageUrl": "https://..." }
```

Leave `slug` blank to derive it from the name.

**Errors:** `409` name or slug already exists · `400` the category still has products

---

## Admin — Orders 🛡️ `/api/admin/orders`

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | All orders; `?status=` filter |
| GET | `/{id}` | Full detail including customer |
| PUT | `/{id}/status` | Advance the status |
| GET | `/statuses` | Every status and its legal next steps |
| GET | `/{id}/notifications` | Delivery attempts for this order |

### `PUT /api/admin/orders/{id}/status` → `200`

```json
{ "status": "DISPATCHED", "note": "Handed to courier, AWB 123456789." }
```

Validated against the state machine. Setting `CANCELLED` restores stock. Every change writes a
history row and triggers a WhatsApp notification.

**Errors:** `400` illegal transition — the message lists what *is* allowed:

```
Cannot change status from 'Order Placed' to 'Delivered'.
Allowed next steps: Cancelled, Order Confirmed.
```

### `GET /api/admin/orders/statuses` → `200`

```json
[
  { "value": "ORDER_PLACED", "label": "Order Placed", "terminal": false,
    "allowedNext": ["CANCELLED", "ORDER_CONFIRMED"] },
  { "value": "DELIVERED", "label": "Delivered", "terminal": true, "allowedNext": [] }
]
```

The admin UI builds its dropdown from this, so it can only ever offer transitions the server accepts.

---

## Admin — Dashboard, Users, Notifications 🛡️ `/api/admin`

| Method | Path | Purpose |
|---|---|---|
| GET | `/stats` | Dashboard aggregates |
| GET | `/users` | Registered users; `?q=` search |
| GET | `/users/{id}` | One user |
| PUT | `/users/{id}/enabled?enabled=` | Enable or disable |
| GET | `/notifications` | Audit log; `?status=` filter |
| GET | `/notifications/summary` | Counts per outcome |

### `GET /api/admin/stats` → `200`

```json
{
  "totalRevenue": 184320.00,
  "revenueLast30Days": 92100.00,
  "totalOrders": 24, "ordersLast30Days": 11,
  "pendingOrders": 6, "deliveredOrders": 15, "cancelledOrders": 3,
  "totalCustomers": 18, "newCustomersLast30Days": 5,
  "totalProducts": 42, "lowStockCount": 4, "totalCategories": 6,
  "ordersByStatus": {
    "ORDER_PLACED": 2, "ORDER_CONFIRMED": 1, "PROCESSING": 1,
    "DISPATCHED": 1, "OUT_FOR_DELIVERY": 1, "DELIVERED": 15, "CANCELLED": 3
  },
  "topSellingProducts": [
    { "productId": 31, "productName": "Wireless Noise Cancelling Headphones",
      "unitsSold": 12, "revenue": 155988.00 }
  ],
  "lowStockProducts": [
    { "productId": 40, "productName": "Adjustable Standing Desk Converter",
      "categoryName": "Furniture", "stock": 2 }
  ],
  "recentOrders": [ ... ]
}
```

Revenue excludes cancelled orders. Low stock means 5 units or fewer.

### `PUT /api/admin/users/{id}/enabled?enabled=false` → `200`

Takes effect on the user's next request, because the JWT filter re-checks the flag against the
database. Orders and reviews are preserved.

**Errors:** `400` you cannot disable your own admin account

### `GET /api/admin/notifications` → `200`

`?status=SENT|FAILED|SKIPPED`

```json
{
  "content": [
    { "id": 91, "channel": "WHATSAPP", "recipient": "+919876543210",
      "message": "Hi Aisha, your order #ORD-20260817-4821 has been dispatched...",
      "triggerStatus": "DISPATCHED", "status": "SENT",
      "providerMessageId": "SM1a2b3c...", "errorMessage": null,
      "createdAt": "2026-08-17T14:00:02Z",
      "orderId": 18, "orderNumber": "ORD-20260817-4821",
      "userId": 7, "userName": "Aisha Sharma" }
  ],
  "page": 0, "totalElements": 91
}
```

---

## Status code summary

| Code | When |
|---|---|
| `200` | Success |
| `201` | Created (register, order, product, address, review) |
| `204` | Deleted, no body |
| `400` | Validation failure or broken business rule |
| `401` | Missing, expired or invalid token |
| `403` | Authenticated but lacking the role |
| `404` | Does not exist, or is not yours |
| `409` | Conflict — duplicate, or insufficient stock |
| `500` | Unexpected server error (logged, generic message returned) |

---

**Next:** [07 — Frontend](07-frontend.md) for how the React app consumes all this.
