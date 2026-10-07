import React from 'react';

/**
 * ErrorBoundary - Captura errores de renderizado en componentes hijos.
 * Evita que un error en una página desmonte toda la aplicación.
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary] Error capturado:', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          padding: '2rem',
          textAlign: 'center'
        }}>
          <div style={{
            background: 'rgba(239,68,68,0.08)',
            border: '1px solid #ef4444',
            borderRadius: '12px',
            padding: '2rem 3rem',
            maxWidth: '500px'
          }}>
            <h2 style={{ color: '#ef4444', marginBottom: '0.5rem' }}>
              Algo salió mal
            </h2>
            <p style={{ color: '#64748b', marginBottom: '1.5rem' }}>
              {this.state.error?.message || 'Error desconocido en el componente.'}
            </p>
            <button
              onClick={() => this.setState({ hasError: false, error: null })}
              style={{
                background: '#6366f1',
                color: '#fff',
                border: 'none',
                borderRadius: '8px',
                padding: '0.75rem 1.5rem',
                cursor: 'pointer',
                fontWeight: 600
              }}
            >
              Reintentar
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
