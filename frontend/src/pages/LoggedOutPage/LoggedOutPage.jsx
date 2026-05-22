import { Link } from 'react-router-dom';

const LoggedOutPage = () => {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#f5f7fb',
      padding: '24px',
      textAlign: 'center',
    }}>
      <div style={{
        background: 'white',
        borderRadius: '16px',
        padding: '32px',
        maxWidth: '520px',
        width: '100%',
        boxShadow: '0 10px 30px rgba(0,0,0,0.08)',
      }}>
        <h1 style={{ marginTop: 0 }}>Sesión cerrada</h1>
        <p style={{ color: '#555' }}>
          Ya saliste de Proyecta. Si deseas volver a entrar, usa el botón de acceso.
        </p>
        <Link
          to="/"
          style={{
            display: 'inline-block',
            marginTop: '16px',
            padding: '12px 20px',
            background: '#0b63ce',
            color: '#fff',
            borderRadius: '8px',
            textDecoration: 'none',
            fontWeight: 600,
          }}
        >
          Volver a entrar
        </Link>
      </div>
    </div>
  );
};

export default LoggedOutPage;
