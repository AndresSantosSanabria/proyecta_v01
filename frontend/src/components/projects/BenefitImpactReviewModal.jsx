import { useEffect, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  ShieldAlert,
  ThumbsDown,
  ThumbsUp,
  X,
} from 'lucide-react';
import projectService from '../../services/projectService';
import './BenefitImpactReviewModal.css';

const AccordionSection = ({ label, value }) => {
  const [open, setOpen] = useState(false);
  return (
    <article className="birm-section">
      <button type="button" onClick={() => setOpen(!open)} className="birm-section-toggle">
        <span>{label}</span>
        {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>
      {open && <div className="birm-section-body">{value || 'No registrado'}</div>}
    </article>
  );
};

const BenefitImpactReviewModal = ({ isOpen, onClose, proyectoId, projectName, benefitImpactData, onSuccess, isDirectorOnly = false }) => {
  const [view, setView] = useState('detail');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [rejectError, setRejectError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setView('detail');
      setRejectReason('');
      setRejectError('');
      setSuccess('');
      setError('');
    }
  }, [isOpen]);

  const handleApprove = async () => {
    if (!proyectoId || loading) return;
    try {
      setLoading(true);
      setError('');
      await projectService.reviewBenefitImpact(proyectoId, true, '');
      setSuccess('Informacion aprobada exitosamente.');
      if (onSuccess) onSuccess();
      setTimeout(onClose, 1200);
    } catch (err) {
      const message =
        err?.response?.data?.detail ||
        err?.response?.data?.message ||
        'No fue posible aprobar la informacion.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleReject = () => {
    setView('rejecting');
  };

  const handleConfirmReject = async () => {
    if (!rejectReason.trim()) {
      setRejectError('Debe ingresar el motivo del rechazo para continuar.');
      return;
    }
    if (!proyectoId || loading) return;
    try {
      setLoading(true);
      setError('');
      await projectService.reviewBenefitImpact(proyectoId, false, rejectReason.trim());
      setSuccess('Informacion rechazada. El Director debe corregirla.');
      if (onSuccess) onSuccess();
      setTimeout(onClose, 1200);
    } catch (err) {
      const message =
        err?.response?.data?.detail ||
        err?.response?.data?.message ||
        'No fue posible rechazar la informacion.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  const data = benefitImpactData;
  const isApproved = data?.estado === 'APROBADO';

  return (
    <div className="birm-overlay" role="presentation" onClick={onClose}>
      <div className="birm-modal" role="dialog" aria-modal="true" aria-labelledby="birm-title" onClick={(e) => e.stopPropagation()}>
        <div className="birm-header">
          <div className="birm-header-text">
            <span className="birm-kicker">
              <ShieldAlert size={14} />
              Revision: Beneficio e Impacto del Proyecto
            </span>
            <h3 id="birm-title">{projectName || 'Proyecto'}</h3>
            <p>{proyectoId || ''}</p>
          </div>
          <button type="button" className="birm-close" onClick={onClose} aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>

        <div className="birm-body">
          {error && (
            <div className="birm-alert error">
              <AlertTriangle size={16} />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="birm-alert success">
              <CheckCircle2 size={16} />
              <span>{success}</span>
            </div>
          )}

          {data && (
            <>
              <div className="birm-state-bar">
                <span className="birm-state-label">Estado</span>
                <span className={`birm-state-badge ${isApproved ? 'approved' : data?.estado === 'OBSERVADO' ? 'observed' : 'pending'}`}>
                  {data?.estado || 'Sin estado'}
                </span>
              </div>

              <AccordionSection label="Beneficios del proyecto" value={data?.beneficioPrincipal} />
              <AccordionSection label="Impactos en valor publico" value={data?.impactoSocial} />
              {data?.observaciones && data.estado !== 'DILIGENCIADO' && (
                <AccordionSection label="Observaciones" value={data?.observaciones} />
              )}
            </>
          )}

          {!data && !error && (
            <div className="birm-loading">
              <div className="birm-skeleton" />
              <div className="birm-skeleton short" />
              <div className="birm-skeleton" />
            </div>
          )}

          {view === 'rejecting' && (
            <div className="birm-reject-section birm-expand">
              <label className="birm-reject-label">Motivo del rechazo / Observaciones para correccion (Obligatorio)</label>
              <textarea
                value={rejectReason}
                onChange={(e) => { setRejectReason(e.target.value); setRejectError(''); }}
                rows={4}
                placeholder="Describe de forma clara qué debe corregir el Director de Proyecto..."
                className={`birm-reject-textarea${rejectError ? ' error' : ''}`}
                autoFocus
              />
              {rejectError && <span className="birm-reject-error">{rejectError}</span>}
            </div>
          )}
        </div>

        <div className="birm-footer">
          {view === 'detail' ? (
            <>
              <button type="button" className="birm-btn secondary" onClick={onClose} disabled={loading}>
                {isApproved ? 'Cerrar' : 'Cancelar'}
              </button>
              {!isApproved && !isDirectorOnly && (
                <>
                  <button type="button" className="birm-btn danger" onClick={handleReject} disabled={loading}>
                    <ThumbsDown size={16} />
                    Rechazar
                  </button>
                  <button type="button" className="birm-btn success" onClick={handleApprove} disabled={loading}>
                    <ThumbsUp size={16} />
                    Aprobar
                  </button>
                </>
              )}
            </>
          ) : (
            <>
              <button type="button" className="birm-btn secondary" onClick={() => setView('detail')} disabled={loading}>
                Volver
              </button>
              <button type="button" className="birm-btn danger" onClick={handleConfirmReject} disabled={loading}>
                {loading ? 'Procesando...' : 'Confirmar Rechazo'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default BenefitImpactReviewModal;
