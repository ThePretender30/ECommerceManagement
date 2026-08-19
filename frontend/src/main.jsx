import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import { CartProvider } from './context/CartContext.jsx'
import { ToastProvider } from './context/ToastContext.jsx'
import './styles/global.css'

/*
 * Provider order matters:
 *   BrowserRouter -> AuthProvider -> CartProvider -> ToastProvider
 *
 * CartProvider reads authentication state to decide whether to load a cart, so
 * it must sit inside AuthProvider. Both sit inside the router because the auth
 * flow navigates.
 */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <ToastProvider>
            <App />
          </ToastProvider>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>
)
