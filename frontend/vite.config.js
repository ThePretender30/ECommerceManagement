import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxying /api to the Spring Boot backend means the browser only ever talks
    // to one origin during development, so no CORS preflight is involved and the
    // frontend can use relative URLs ("/api/products") in every environment.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
