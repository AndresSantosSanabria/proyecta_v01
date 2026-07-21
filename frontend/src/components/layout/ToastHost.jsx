import { useEffect, useState } from 'react';
import { CheckCircle2, AlertTriangle, Info, X } from 'lucide-react';

const TOAST_EVENT = 'proyecta:toast';
const DEFAULT_TIMEOUT = 3500;

const iconByTone = {
  success: CheckCircle2,
  error: AlertTriangle,
  warning: AlertTriangle,
  info: Info,
};

const ToastHost = () => {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const handleToast = (event) => {
      const detail = event?.detail || {};
      const id = detail.id || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      const toast = {
        id,
        title: detail.title || 'Notificación',
        message: detail.message || '',
        tone: detail.tone || 'info',
      };

      setToasts((current) => [...current, toast]);

      window.setTimeout(() => {
        setToasts((current) => current.filter((item) => item.id !== id));
      }, detail.duration || DEFAULT_TIMEOUT);
    };

    window.addEventListener(TOAST_EVENT, handleToast);
    return () => window.removeEventListener(TOAST_EVENT, handleToast);
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div style={{
      position: 'fixed',
      right: '16px',
      bottom: '16px',
      zIndex: 9999,
      display: 'flex',
      flexDirection: 'column',
      gap: '10px',
      maxWidth: '360px',
    }}>
      {toasts.map((toast) => {
        const Icon = iconByTone[toast.tone] || Info;
        return (
          <article
            key={toast.id}
            style={{
              display: 'flex',
              gap: '10px',
              alignItems: 'flex-start',
              background: 'rgba(15, 23, 42, 0.96)',
              color: '#e2e8f0',
              border: '1px solid rgba(148, 163, 184, 0.25)',
              boxShadow: '0 16px 40px rgba(0, 0, 0, 0.35)',
              borderRadius: '14px',
              padding: '12px 14px',
              backdropFilter: 'blur(10px)',
            }}
          >
            <div style={{ color: toast.tone === 'success' ? '#22c55e' : toast.tone === 'error' ? '#ef4444' : '#60a5fa', marginTop: '2px' }}>
              <Icon size={18} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <strong style={{ display: 'block', fontSize: '0.95rem', marginBottom: '2px' }}>{toast.title}</strong>
              {toast.message ? <p style={{ margin: 0, fontSize: '0.88rem', lineHeight: 1.35, color: '#cbd5e1' }}>{toast.message}</p> : null}
            </div>
            <button
              type="button"
              onClick={() => setToasts((current) => current.filter((item) => item.id !== toast.id))}
              aria-label="Cerrar notificación"
              style={{
                background: 'transparent',
                border: 'none',
                color: '#94a3b8',
                cursor: 'pointer',
                padding: 0,
                lineHeight: 0,
              }}
            >
              <X size={16} />
            </button>
          </article>
        );
      })}
    </div>
  );
};

export default ToastHost;
