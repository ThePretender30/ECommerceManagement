import { Route, Routes, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import { AdminRoute, GuestRoute, ProtectedRoute } from './components/RouteGuards'

// Public pages
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import Categories from './pages/Categories'
import ProductList from './pages/ProductList'
import ProductDetail from './pages/ProductDetail'
import NotFound from './pages/NotFound'

// Customer pages
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import OrderConfirmation from './pages/OrderConfirmation'
import MyOrders from './pages/MyOrders'
import OrderTracking from './pages/OrderTracking'
import Profile from './pages/Profile'
import Addresses from './pages/Addresses'

// Admin pages
import AdminLayout from './pages/admin/AdminLayout'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminProducts from './pages/admin/AdminProducts'
import AdminProductForm from './pages/admin/AdminProductForm'
import AdminCategories from './pages/admin/AdminCategories'
import AdminOrders from './pages/admin/AdminOrders'
import AdminOrderDetail from './pages/admin/AdminOrderDetail'
import AdminUsers from './pages/admin/AdminUsers'
import AdminNotifications from './pages/admin/AdminNotifications'

/**
 * The route table.
 *
 * Routes fall into four groups:
 *   - public   : browsing the storefront, no account needed
 *   - guest    : login and register, redirected away once signed in
 *   - protected: anything tied to a specific person (cart, orders, profile)
 *   - admin    : the dashboard, additionally requiring ROLE_ADMIN
 *
 * The guards are UX only - the backend enforces the same rules independently.
 */
export default function App() {
  const location = useLocation()

  // The admin area brings its own sidebar chrome, so the storefront navbar and
  // footer are hidden there.
  const isAdminArea = location.pathname.startsWith('/admin')

  return (
    <div className="app-shell">
      {!isAdminArea && <Navbar />}

      <main>
        <Routes>
          {/* ---------------- Public ---------------- */}
          <Route path="/" element={<Home />} />
          <Route path="/categories" element={<Categories />} />
          <Route path="/products" element={<ProductList />} />
          <Route path="/products/:id" element={<ProductDetail />} />

          {/* ---------------- Guest only ------------ */}
          <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
          <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />

          {/* ---------------- Signed in ------------- */}
          <Route path="/cart" element={<ProtectedRoute><Cart /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
          <Route
            path="/orders/:id/confirmation"
            element={<ProtectedRoute><OrderConfirmation /></ProtectedRoute>}
          />
          <Route path="/orders" element={<ProtectedRoute><MyOrders /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderTracking /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
          <Route path="/addresses" element={<ProtectedRoute><Addresses /></ProtectedRoute>} />

          {/* ---------------- Admin ----------------- */}
          <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
            <Route index element={<AdminDashboard />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="products/new" element={<AdminProductForm />} />
            <Route path="products/:id/edit" element={<AdminProductForm />} />
            <Route path="categories" element={<AdminCategories />} />
            <Route path="orders" element={<AdminOrders />} />
            <Route path="orders/:id" element={<AdminOrderDetail />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="notifications" element={<AdminNotifications />} />
          </Route>

          {/* ---------------- Fallback -------------- */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>

      {!isAdminArea && <Footer />}
    </div>
  )
}
