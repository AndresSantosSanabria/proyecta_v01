import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from './context/ThemeContext.jsx'
import { AuthProvider as AppAuthProvider } from './context/AuthContext.jsx'
import { auth, decodeJwtPayload } from './utils/auth'
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
    const payload = decodeJwtPayload(user?.access_token)
    console.log('[AUTH START] user =', user)
    console.log('[AUTH START] access_token exists =', Boolean(user?.access_token))
    console.log('[AUTH START] access_token length =', user?.access_token?.length ?? 0)
    console.log('[AUTH START] token summary =', {
      iss: payload?.iss,
      azp: payload?.azp,
      exp: payload?.exp,
      preferred_username: payload?.preferred_username,
      realm_roles: payload?.realm_access?.roles ?? [],
      resource_roles: payload?.resource_access,
    })
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

