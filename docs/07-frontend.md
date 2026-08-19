# 07 — Frontend

The React application: structure, routing, state, and the CSS system.

---

## Stack

| Choice | Version | Why |
|---|---|---|
| React | 19 | |
| React Router | 7 | Client-side routing |
| Vite | 8 | Fast dev server; proxies `/api` so CORS never fires in development |
| axios | 1.x | One instance with interceptors doing the cross-cutting work |
| CSS | hand-written | Design tokens + flexbox/grid. No UI library, so nothing to fight and nothing to learn beyond CSS. |

---

## Folder structure

```
frontend/src/
├─ main.jsx                 provider nesting and root render
├─ App.jsx                  the route table
├─ components/              reusable UI, no page-specific knowledge
│  ├─ Navbar.jsx/.css
│  ├─ Footer.jsx/.css
│  ├─ ProductCard.jsx/.css      also defines .product-grid
│  ├─ Common.jsx/.css           Loader, EmptyState, ErrorState, StarRating,
│  │                            QuantityStepper, Pagination, Breadcrumbs
│  ├─ Modal.jsx/.css            Modal + ConfirmDialog
│  ├─ Toast.jsx/.css
│  ├─ OrderStatusTimeline.jsx/.css
│  └─ RouteGuards.jsx           ProtectedRoute, AdminRoute, GuestRoute
├─ pages/                   one component per route
│  ├─ Home, Login, Register, Categories, ProductList, ProductDetail
│  ├─ Cart, Checkout, OrderConfirmation, MyOrders, OrderTracking
│  ├─ Profile, Addresses, NotFound
│  └─ admin/
│     ├─ AdminLayout        sidebar shell wrapping all admin routes
│     ├─ AdminDashboard, AdminProducts, AdminProductForm
│     ├─ AdminCategories, AdminOrders, AdminOrderDetail
│     └─ AdminUsers, AdminNotifications
├─ services/                API layer
│  ├─ api.js                the axios instance + interceptors + token storage
│  ├─ authService.js
│  ├─ productService.js
│  ├─ catalogService.js     categories, cart, addresses
│  ├─ orderService.js
│  └─ adminService.js
├─ context/                 global state
│  ├─ AuthContext.jsx
│  ├─ CartContext.jsx
│  └─ ToastContext.jsx
├─ hooks/index.js           useAuth, useCart, useToast, useDebounce, useQueryParams
├─ styles/
│  ├─ tokens.css            every colour, space, radius, shadow
│  └─ global.css            reset, base elements, shared utility classes
└─ utils/format.js          currency, dates, status badges, image fallback
```

Each component keeps its CSS beside it. Only genuinely shared rules — buttons, forms, cards,
tables, badges — live in `global.css`.

---

## Provider nesting

```jsx
<BrowserRouter>
  <AuthProvider>
    <CartProvider>
      <ToastProvider>
        <App />
```

The order is not arbitrary: `CartProvider` reads authentication state to decide whether to fetch a
cart, so it must sit inside `AuthProvider`. Both are inside the router because the auth flow
navigates.

---

## Routing

`App.jsx` holds the whole route table, grouped by protection level.

| Route | Page | Guard |
|---|---|---|
| `/` | Home | — |
| `/categories` | Categories | — |
| `/products` | ProductList | — |
| `/products/:id` | ProductDetail | — |
| `/login` | Login | Guest |
| `/register` | Register | Guest |
| `/cart` | Cart | Protected |
| `/checkout` | Checkout | Protected |
| `/orders` | MyOrders | Protected |
| `/orders/:id` | OrderTracking | Protected |
| `/orders/:id/confirmation` | OrderConfirmation | Protected |
| `/profile` | Profile | Protected |
| `/addresses` | Addresses | Protected |
| `/admin` | AdminDashboard | **Admin** |
| `/admin/products` | AdminProducts | Admin |
| `/admin/products/new` | AdminProductForm | Admin |
| `/admin/products/:id/edit` | AdminProductForm | Admin |
| `/admin/categories` | AdminCategories | Admin |
| `/admin/orders` | AdminOrders | Admin |
| `/admin/orders/:id` | AdminOrderDetail | Admin |
| `/admin/users` | AdminUsers | Admin |
| `/admin/notifications` | AdminNotifications | Admin |
| `*` | NotFound | — |

The admin routes are nested under a layout route, so `AdminLayout` renders its sidebar once and each
page appears in its `<Outlet />`. `App.jsx` also hides the storefront navbar and footer under
`/admin`, because the dashboard is a separate workspace.

### The guards are UX, not security

```jsx
export function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()
  if (loading) return <Loader fullPage />
  if (!isAuthenticated) return <Navigate to={`/login?redirect=${...}`} replace />
  if (!isAdmin) return <Navigate to="/" replace />
  return children
}
```

> Disabling these in devtools reveals **empty pages, not data**. Every admin endpoint is enforced
> independently by Spring Security. The guards exist so a signed-out user sees a login form instead
> of a page that immediately fails with a 401.

Two details worth copying:

- **They wait for `loading`.** Without that check, refreshing a protected page would bounce the user
  to login before the token had been validated.
- **They preserve the destination** in `?redirect=`, so signing in returns you where you were going.

---

## The API layer

### `api.js` — one instance, two interceptors

```js
const api = axios.create({ baseURL: '/api', timeout: 20000 })
```

A **relative** base URL: Vite proxies `/api` to port 8080 in development, and in production the app
is served from the same origin as the API. No environment-specific URL anywhere.

**Request interceptor** attaches the token, so no individual call has to remember:

```js
api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

**Response interceptor** does three jobs:

1. **Turns a network failure into a readable message.** No response at all means the backend is
   down — worth saying so rather than showing "Network Error".
2. **Handles 401 by signing out** — but *not* 403, and not for a failed login attempt:

   ```js
   if (status === 401) {
     const isLoginAttempt = url.includes('/auth/login') || url.includes('/auth/register')
     if (!isLoginAttempt) { tokenStorage.clear(); onUnauthorized?.() }
   }
   ```

   Both exclusions matter. Signing the user out because a customer touched an admin URL (403) would
   be wrong — they are legitimately signed in. And a wrong password at the login screen is an
   expected 401, not an expired session.

3. **Unwraps the backend's `ApiError`** into a plain `Error` with a readable `.message` and an
   optional `.fieldErrors`. Because the backend always returns one error shape, every component
   renders failures the same way:

   ```js
   try { await addItem(product.id, 1) }
   catch (error) { toast.error(error.message) }
   ```

### Feature services

Thin wrappers, one per API area. `productService.list` strips empty values so the URL only carries
filters that are actually applied:

```js
list: (params = {}) => {
  const query = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query[key] = value
  })
  return api.get('/products', { params: query }).then((r) => r.data)
}
```

---

## State management

Three contexts. No Redux — the app's shared state is small and clearly bounded.

### `AuthContext`

Holds the current user. Two subtleties:

**It caches the user in `localStorage` and then revalidates.** The cache prevents the app flashing
back to a signed-out state on every refresh; the revalidation against `/auth/me` is what actually
establishes whether the token is still good.

**It registers a handler with the axios interceptor:**

```js
useEffect(() => {
  setUnauthorizedHandler(() => { tokenStorage.clear(); setUser(null) })
}, [])
```

A callback rather than `window.location = '/login'`, which would do a full page reload and throw
away the SPA's router state.

Exposes: `user`, `loading`, `isAuthenticated`, `isAdmin`, `login`, `register`, `logout`,
`updateUser`.

### `CartContext`

**Mirrors the server; it never patches its own copy.** Every mutation calls the API and replaces
local state with the response:

```js
const addItem = useCallback(async (productId, quantity = 1) => {
  const updated = await cartService.addItem(productId, quantity)
  setCart(updated)          // whatever the server says is the truth
  return updated
}, [])
```

That is why the navbar badge, cart page and checkout total can never disagree, and why the cart
follows the user across devices.

It also loads on sign-in and clears on sign-out, driven by `isAuthenticated`.

### `ToastContext`

Global notifications. In context rather than per-page state so an action that navigates away —
placing an order, deleting a product — can still report its result on the page you land on.

---

## Hooks

| Hook | Purpose |
|---|---|
| `useAuth` / `useCart` / `useToast` | Context accessors; throw a clear error if used outside their provider |
| `useDebounce(value, delay)` | Delays a fast-changing value — search-as-you-type fires one request when you pause, not one per keystroke |
| `useQueryParams()` | Reads and writes filter state through the URL |
| `useScrollToTop(dep)` | Scrolls up on navigation |

### Why filters live in the URL

`useQueryParams` is the most consequential of these:

```js
const { get, setParams, clearAll } = useQueryParams()
const sort = get('sort', 'newest')
setParams({ category: 'electronics', brand: null })   // null removes the parameter
```

Keeping filters, sort and page in the query string means results are **shareable and bookmarkable**,
and the browser's back button steps through filter changes the way users expect. None of that works
if that state lives only in React.

One deliberate behaviour: changing any filter resets to page 1, unless you explicitly pass
`{ resetPage: false }`. Otherwise narrowing a search while on page 5 lands you on an empty page.

---

## Component catalogue

| Component | Notes |
|---|---|
| `Navbar` | Brand, search, account dropdown, cart badge. Collapses to a drawer under 1024px. |
| `Footer` | Category links, account links, project note. |
| `ProductCard` | Product tile with inline add-to-cart. Also defines `.product-grid`, used by every listing. |
| `Loader` | Spinner; `fullPage` centres it. |
| `EmptyState` | For legitimately empty lists — distinct from an error, and suggests what to do next. |
| `ErrorState` | Failure panel with an optional retry. |
| `StarRating` | Renders half-stars by clipping a filled row over an empty one, so 3.5 is not rounded to 4. |
| `QuantityStepper` | +/− control with min/max clamping. |
| `Pagination` | Windowed: at most five numbered buttons, so the control is a fixed width at 3 pages or 300. |
| `Breadcrumbs` | Hierarchy trail. |
| `Modal` / `ConfirmDialog` | Rendered through a portal into `document.body` — escapes any ancestor `overflow: hidden`, which is the usual reason a modal appears clipped. Closes on Escape and locks background scroll. |
| `Toast` | The toast stack. `aria-live="polite"` so screen readers announce without interrupting. |
| `OrderStatusTimeline` | Renders the tracking steps. **Contains no knowledge of the delivery sequence** — the backend supplies each step's state. Vertical on mobile, horizontal from 768px. |
| `ProtectedRoute` / `AdminRoute` / `GuestRoute` | Route guards. |

---

## Styling

### Design tokens

Every colour, space, radius, shadow and font size is a CSS custom property in `styles/tokens.css`.
Components reference tokens, never raw values, so the whole look can be re-themed from one file.

```css
:root {
  --color-primary: #2563eb;
  --space-4: 1rem;
  --radius-md: 12px;
  --shadow-md: 0 4px 6px -1px rgba(15, 23, 42, 0.1), ...;
  --text-lg: 1.125rem;
}
```

Spacing uses a 4px base scale; type uses a matching modular scale.

### Responsive approach

Mobile-first — base styles target phones, and `min-width` media queries add complexity upward.
Three breakpoints, used consistently:

| Breakpoint | Target | What changes |
|---|---|---|
| `640px` | Large phone | Grids go 2 → 3 columns; toasts move to a corner; modals centre |
| `768px` | Tablet | Product detail becomes two columns; the tracking timeline goes horizontal |
| `1024px` | Laptop | Navbar links appear; filter sidebar becomes persistent; admin sidebar becomes fixed |

Concrete behaviours:

- **Product grid:** 2 columns → 3 at 640px → 4 at 1024px
- **Navbar:** burger drawer with search on its own row → single row at 1024px
- **Filters:** collapsible panel → sticky sidebar at 1024px
- **Admin sidebar:** slide-in drawer with a dimmed overlay → permanent at 1024px
- **Tables:** wrapped in `.table-wrap` with `overflow-x: auto`, so a wide admin table scrolls
  *inside its own box* rather than forcing the page to scroll sideways
- **Cart items:** CSS Grid template areas reflow from stacked to a single row at 640px

### Accessibility

- Focus outlines are restyled, never removed (`:focus-visible`)
- `prefers-reduced-motion` disables animations and smooth scrolling
- Icon-only buttons carry `aria-label`; decorative emoji carry `aria-hidden`
- `.sr-only` for screen-reader-only text
- Dropdowns and drawers use `aria-expanded`; modals use `role="dialog"` and `aria-modal`
- Colour is never the sole signal — status badges pair colour with text

---

## Patterns used consistently

**Load / error / empty / content.** Every data-driven page handles all four states:

```jsx
if (loading) return <Loader fullPage />
if (error)   return <ErrorState message={error} onRetry={load} />
if (result?.empty) return <EmptyState ... />
return <div>{/* content */}</div>
```

**Parallel independent requests.** Where a page needs several unrelated calls, they go together
rather than in a waterfall:

```jsx
Promise.all([
  categoryService.list(),
  productService.featured(),
  productService.popular(),
  productService.newArrivals(),
]).then(([categories, featured, popular, newArrivals]) => ...)
```

**Field errors from the backend, rendered inline.** The `fieldErrors` map drives per-input messages,
so validation rules are defined once — on the server — and the form does not duplicate them:

```jsx
<input className={fieldErrors.email ? 'form-control has-error' : 'form-control'} ... />
{fieldErrors.email && <span className="form-error">{fieldErrors.email}</span>}
```

**Image fallback.** `handleImageError` swaps in an inline SVG placeholder once, guarding against a
loop if the fallback itself fails.

---

## Build

```powershell
npm run dev       # dev server on :5173 with the /api proxy
npm run build     # production bundle into dist/
npm run preview   # serve the built bundle on :4173
npm run lint      # oxlint
```

Current production build: ~418 KB JS (118 KB gzipped), ~57 KB CSS (9 KB gzipped).

`npm run lint` reports three `react(only-export-components)` warnings — each context file exports
both the context object and its provider. That only affects Fast Refresh granularity during
development, not correctness. Splitting them into separate files would silence it at the cost of
three extra files.

---

**Next:** [08 — Business Flows](08-business-flows.md) for the sequence diagrams behind these screens.
