import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from './context/ThemeContext.jsx'
import { AuthProvider as AppAuthProvider } from './context/AuthContext.jsx'
import './index.css'
import App from './App.jsx'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
  },
});

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppAuthProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          {/* BASE_URL = prefijo de despliegue (Vite `base`, ej. /apps/proyecta/public/) */}
          <BrowserRouter basename={import.meta.env.BASE_URL}>
            <App />
          </BrowserRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </AppAuthProvider>
  </StrictMode>,
)

