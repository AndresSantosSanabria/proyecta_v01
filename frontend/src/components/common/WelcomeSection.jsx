import { useEffect, useState } from 'react';
import { useApiWithAuth } from '../../hooks/useApiWithAuth';
import { useAuthContext } from '../../context/AuthContext';

function WelcomeSection() {
  const { user, logout, roles } = useAuthContext();
  const { callApi } = useApiWithAuth();
  const [bienvenida, setBienvenida] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchBienvenida = async () => {
      try {
        setLoading(true);
        const data = await callApi('/app/bienvenida');
        setBienvenida(data);
        setError(null);
      } catch (err) {
        console.error('Error al llamar /app/bienvenida:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchBienvenida();
  }, [callApi]);

  return (
    <div style={{ padding: '20px', maxWidth: '600px', margin: '0 auto' }}>
      <h1>Bienvenido a Proyecta</h1>

      <div style={{
        backgroundColor: '#e3f2fd',
        padding: '15px',
        borderRadius: '8px',
        marginBottom: '20px'
      }}>
        <h3>Información del Usuario:</h3>
        <p><strong>Email:</strong> {user?.profile?.email}</p>
        <p><strong>Nombre:</strong> {user?.profile?.name}</p>
        <p><strong>Usuario:</strong> {user?.profile?.preferred_username}</p>
        <p><strong>Roles Keycloak:</strong> {roles.length ? roles.join(', ') : 'Sin roles detectados'}</p>
      </div>

      {loading && <p>Cargando datos del servidor...</p>}
      {error && (
        <div style={{
          backgroundColor: '#ffebee',
          padding: '15px',
          borderRadius: '8px',
          color: '#c62828',
          marginBottom: '20px'
        }}>
          <strong>Error:</strong> {error}
        </div>
      )}
      {bienvenida && (
        <div style={{
          backgroundColor: '#f3e5f5',
          padding: '15px',
          borderRadius: '8px',
          marginBottom: '20px'
        }}>
          <h3>Respuesta del Servidor:</h3>
          <p><strong>Mensaje:</strong> {bienvenida.mensaje}</p>
          <p><strong>Usuario (Backend):</strong> {bienvenida.usuario}</p>
          <p><strong>Roles:</strong> {bienvenida.roles?.join(', ')}</p>
        </div>
      )}

      <button
        onClick={() => logout()}
        style={{
          padding: '10px 20px',
          backgroundColor: '#f44336',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '16px'
        }}
      >
        Cerrar Sesión
      </button>
    </div>
  );
}

export default WelcomeSection;

