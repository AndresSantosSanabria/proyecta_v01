import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from './context/ThemeContext.jsx'
import { AuthProvider as AppAuthProvider } from './context/AuthContext.jsx'
import { auth, extractRolesFromToken } from './utils/auth'
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

auth.getUser()
  .then((user) => {
    const tokenRoles = extractRolesFromToken(user?.access_token)
    console.info('[AUTH START] roles token =', tokenRoles)
  })
  .catch((error) => {
    console.error('[AUTH START] error reading user =', error)
  })

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppAuthProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </AppAuthProvider>
  </StrictMode>,
)

