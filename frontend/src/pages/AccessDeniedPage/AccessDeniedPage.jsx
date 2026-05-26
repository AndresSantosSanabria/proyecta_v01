import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const AccessDeniedPage = () => {
  const navigate = useNavigate();

  return (
    <div style={{
      minHeight: '100vh',
      display: 'grid',
      placeItems: 'center',
      padding: '32px',
      background: 'radial-gradient(circle at top, rgba(239, 68, 68, 0.12), transparent 40%), var(--bg-main)',
    }}>
      <div style={{
        maxWidth: '620px',
        width: '100%',
        background: 'var(--bg-card)',
        border: '1px solid var(--border-color)',
        borderRadius: '28px',
        boxShadow: 'var(--shadow-premium)',
        padding: '36px',
        textAlign: 'center',
      }}>
        <div style={{
          width: '84px',
          height: '84px',
          borderRadius: '24px',
          display: 'grid',
          placeItems: 'center',
          margin: '0 auto 20px',
          background: 'rgba(239, 68, 68, 0.12)',
          color: 'var(--danger)',
        }}>
          <ShieldAlert size={40} />
        </div>
        <h1 style={{ fontSize: '2rem', marginBottom: '10px' }}>Acceso denegado</h1>
        <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>
          Tu usuario no tiene permisos funcionales o no está asignado al proyecto solicitado.
        </p>
        <button
          onClick={() => navigate(-1)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '12px 18px',
            borderRadius: '12px',
            border: 'none',
            background: 'var(--primary-color)',
            color: '#fff',
            cursor: 'pointer',
            fontWeight: 700,
          }}
        >
          <ArrowLeft size={16} />
          Volver
        </button>
      </div>
    </div>
  );
};

export default AccessDeniedPage;
