import { useState } from 'react';
import emailNotificationService from '../../services/emailNotificationService';

/**
 * Ejemplo de uso del servicio de notificaciones por correo con hilo por proyecto.
 * El backend guarda el Message-ID raíz en la primera petición y encadena
 * las siguientes con In-Reply-To / References dentro del mismo hilo Outlook.
 */
const EmailNotificationExample = ({ projectId }) => {
  const [mensaje, setMensaje] = useState('');
  const [sending, setSending] = useState(false);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);

  const handleSend = async () => {
    if (!mensaje.trim()) {
      setError('Escribe un mensaje antes de enviar.');
      return;
    }
    try {
      setSending(true);
      setError(null);
      setStatus(null);
      const result = await emailNotificationService.sendThreadedEmail(projectId, mensaje);
      setStatus(result?.messageId ?? 'Enviado');
      setMensaje('');
    } catch (err) {
      console.error('Error enviando correo del proyecto:', err);
      setError(err?.response?.data?.message || 'No fue posible enviar el correo.');
    } finally {
      setSending(false);
    }
  };

  return (
    <div>
      <h3>Notificación por correo (hilo del proyecto)</h3>
      <textarea
        rows={4}
        value={mensaje}
        onChange={(e) => setMensaje(e.target.value)}
        placeholder="Mensaje del correo..."
      />
      <button type="button" onClick={handleSend} disabled={sending}>
        {sending ? 'Enviando…' : 'Enviar correo'}
      </button>
      {status && <p>Message-ID: {status}</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
    </div>
  );
};

export default EmailNotificationExample;
