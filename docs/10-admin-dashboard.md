# 10 — Admin Dashboard

Each admin screen, the endpoints behind it, and the rules it enforces.

---

## Access

The admin area lives under `/admin` and requires `ROLE_ADMIN`.

Two independent layers:

- **`AdminRoute`** (frontend) redirects non-admins to the home page.
- **`SecurityConfig`** (backend) gates every `/api/admin/**` endpoint with `hasRole("ADMIN")`.

Only the second is security. Bypassing the guard in devtools reveals **empty pages**, because every
underlying request returns 403.

There is no separate admin login — the same `/login` page serves everyone, and admins are routed to
`/admin` afterwards based on their roles.

The admin area uses its own layout (`AdminLayout`): a sidebar plus top bar, with the storefront
navbar and footer hidden. On screens under 1024px the sidebar becomes a slide-in drawer.

---

## Dashboard — `/admin`

**Endpoint:** `GET /api/admin/stats` → `AdminStatsService.getDashboardStats()`

Every figure is a live database aggregate. Nothing is placeholder data.

### Headline cards

| Card | Source |
|---|---|
| Total revenue | `SUM(total_amount)` excluding cancelled orders |
| Total orders | Sum of the per-status counts |
| Pending orders | Everything between `ORDER_PLACED` and `OUT_FOR_DELIVERY` |
| Customers | `COUNT(*)` on `users` |
| Active products | `COUNT(*)` where `active = true` |
| Low stock | Products with stock ≤ 5 |
| Delivered / Cancelled | Per-status counts |

Revenue and the "last 30 days" figures come from `calculateTotalRevenue()` and
`calculateRevenueSince(...)`. **Cancelled orders are excluded from revenue** — the shop did not earn
that money.

### Orders by status

A single grouped query, not seven separate counts:

```java
@Query("SELECT o.status AS status, COUNT(o) AS count FROM Order o GROUP BY o.status")
List<StatusCount> countGroupedByStatus();
```

The service seeds the map with every status at zero first, so a status with no orders still renders
its (empty) bar rather than disappearing.

### Best sellers

Top five by **units shipped**, cancelled orders excluded:

```java
SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.lineTotal)
FROM OrderItem oi
WHERE oi.order.status <> CANCELLED AND oi.product IS NOT NULL
GROUP BY oi.product.id, oi.productName
ORDER BY SUM(oi.quantity) DESC
```

Note it reads `oi.productName` — the **snapshot** — so a renamed product still shows under the name
it sold as.

### Recent orders and low stock

The eight newest orders, and every product at or below the low-stock threshold
(`ProductService.LOW_STOCK_THRESHOLD = 5`), each linking to the relevant management screen.

---

## Products — `/admin/products`

**Endpoints:** `GET|POST|PUT|DELETE /api/admin/products`, `PATCH /api/admin/products/{id}/stock`

A paginated table with search and a category filter.

**This listing includes deactivated products**, unlike the storefront — that is what
`includeInactive = true` does in `ProductService.search`. Without it, a soft-deleted product would
be invisible to the person who needs to manage it.

### Inline stock editing

Click a stock badge to turn it into an input, then Save. Colour-coded: green (>5), amber (1–5), red
(0). This calls `PATCH .../stock` rather than the full update endpoint, so restocking does not
require opening the whole product form.

### Delete behaviour

`DELETE /api/admin/products/{id}` does one of two things, and **tells you which**:

| Situation | Action | Response message |
|---|---|---|
| The product appears in any order | `active = false` | *"This product appears in existing orders, so it was deactivated and hidden from the store rather than deleted."* |
| Never ordered | Row removed | *"Product deleted."* |

Deciding automatically, and reporting it, matters: an admin who deletes a product and still sees it
in order history would otherwise think the system was broken.

---

## Add / edit product — `/admin/products/new`, `/admin/products/:id/edit`

One component serves both; the presence of an `:id` param decides the mode.

| Field | Rules |
|---|---|
| Name | Required, ≤ 200 chars |
| Description | ≤ 5000 chars |
| Price | Required, > 0, max 8 digits + 2 decimals |
| Category | Required |
| Brand | Optional |
| Image URL | Optional link (there are no uploads) |
| Stock | Required, ≥ 0 |
| Visible in store | The `active` flag |

**There are no rating fields.** `ProductRequest` has no `averageRating` or `reviewCount`, so an
admin cannot fabricate a five-star product with no reviews. Those values are derived from real
customer reviews only.

A live preview panel renders the product card as it will appear, so an admin can check the image URL
resolves before saving.

---

## Categories — `/admin/categories`

**Endpoints:** `GET|POST|PUT|DELETE /api/admin/categories`

Table with product counts, and a modal for create/edit.

**Slug:** leave it blank and `SlugUtils.toSlug` derives it from the name
("Sports Equipment" → `sports-equipment`). Restricted to lowercase letters, numbers and hyphens so
it is always URL-safe.

**Delete is refused while products reference the category.** The backend returns:

```
Cannot delete 'Electronics' because 7 product(s) still belong to it.
Move or delete those products first.
```

The button is also disabled in the UI, with the reason as a tooltip.

> Refusing rather than cascading is deliberate. Cascading here would let one click silently delete a
> shelf full of live products.

---

## Orders — `/admin/orders`

**Endpoints:** `GET /api/admin/orders`, `GET /api/admin/orders/{id}`

Every order across all customers, filterable by status, newest first. Each row shows the order
number, customer, date, item count, status badge and total.

Unlike customer endpoints, these are **not** scoped by user — but they require `ROLE_ADMIN`.

---

## Order detail — `/admin/orders/:id`

The most functional admin screen.

**Endpoints:**
`GET /api/admin/orders/{id}` · `PUT /api/admin/orders/{id}/status` ·
`GET /api/admin/orders/statuses` · `GET /api/admin/orders/{id}/notifications`

### Status control

The dropdown is built from the backend's own state machine:

```js
const currentStatusInfo = statuses.find((s) => s.value === order.status)
const allowedNext = currentStatusInfo?.allowedNext ?? []
```

`GET /api/admin/orders/statuses` returns each status with its `allowedNext` array. So the UI can
only ever *offer* transitions the server would accept — an illegal jump is unselectable rather than
merely rejected after the fact. When an order reaches `DELIVERED` or `CANCELLED`, the form is
replaced with a note that the order is final.

An optional note is recorded in the status history.

Selecting `CANCELLED` shows a warning that every item will be returned to stock.

**Every status change triggers a WhatsApp notification** — that is what the "Update and notify
customer" button label is telling you.

### Notification log

Every delivery attempt for this order, with the full message body, outcome badge, recipient and
timestamp — including `SKIPPED` ones. This is where you check whether the customer was actually
told.

### Customer and delivery panels

The customer's account details, and the **snapshot** of the delivery address as it was at checkout —
not their current saved address.

---

## Users — `/admin/users`

**Endpoints:** `GET /api/admin/users`, `PUT /api/admin/users/{id}/enabled`

Searchable across name, email and phone.

### Enable / disable, never delete

There is no delete. Disabling preserves the user's orders and reviews, which are business records.

Disabling **takes effect on the user's very next request**, because `JwtAuthenticationFilter`
re-loads the user from the database and re-checks the `enabled` flag on every call. Without that,
a disabled account would keep working until its token expired — up to 24 hours.

An admin cannot disable their own account. Enforced in `UserService.setEnabled` (400) and the
button is disabled in the UI, so nobody can lock themselves out.

---

## Notifications — `/admin/notifications`

**Endpoints:** `GET /api/admin/notifications`, `GET /api/admin/notifications/summary`

The full audit log across all orders, filterable by outcome.

Summary cards show Sent / Failed / Skipped counts. When everything is `SKIPPED` and nothing is
`SENT`, the page explains why:

> WhatsApp sending is currently inactive, so messages are being logged instead of delivered. Set
> `WHATSAPP_ENABLED=true` along with your Twilio credentials in `backend/.env` to send real
> messages.

Failed rows carry the provider error as a tooltip; sent rows carry the Twilio message SID.

See [09 — WhatsApp Notifications](09-whatsapp-notifications.md) for the debugging workflow.

---

## Common admin tasks

| Task | Where |
|---|---|
| Restock a product | Products → click the stock badge → edit → Save |
| Hide a product without deleting it | Products → Edit → untick "Visible in the store" |
| Confirm and dispatch an order | Orders → open it → Update status |
| See why a customer says they got no message | Orders → open it → the notification log |
| Find which products need reordering | Dashboard → Low stock |
| Suspend an abusive account | Users → Disable |
| Add a new department | Categories → Add category |

---

**Next:** [11 — Troubleshooting](11-troubleshooting.md).
