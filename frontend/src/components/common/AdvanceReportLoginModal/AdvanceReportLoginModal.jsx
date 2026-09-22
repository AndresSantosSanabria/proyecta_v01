import { useState, useEffect, useCallback } from 'react';
import { AlertTriangle, Clock, FileText, ExternalLink, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import advanceReportService from '../../../services/advanceReportService';
import './AdvanceReportLoginModal.css';

const AdvanceReportLoginModal = ({ isOpen, onClose }) => {
  const [pendingProjects, setPendingProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadPending = useCallback(async () => {
    try {
      setLoading(true);
      const data = await advanceReportService.getPendingProjects();
      setPendingProjects(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error loading pending advance reports:', err);
      setPendingProjects([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOpen) loadPending();
  }, [isOpen, loadPending]);

  const handleNavigate = (projectId) => {
    navigate(`/projects/${encodeURIComponent(projectId)}/progress?openReportUpload=true`);
    onClose();
  };

  if (!isOpen || pendingProjects.length === 0) return null;

  const firstProject = pendingProjects[0];
  const dueDate = firstProject?.dueDate;
  const isOverdue = firstProject?.isOverdue;

  return (
    <div className="arm-overlay" onClick={onClose}>
      <div className="arm-modal" onClick={(e) => e.stopPropagation()}>
        <div className="arm-header">
          <div className="arm-header__icon">
            <AlertTriangle size={20} />
          </div>
          <div className="arm-header__text">
            <h3>Informe de avance pendiente</h3>
            <p>{pendingProjects.length} proyecto(s) con informe sin cargar</p>
          </div>
          <button className="arm-close" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        <div className="arm-body">
          {loading ? (
            <div className="arm-loading">Cargando proyectos...</div>
          ) : (
            <>
              {dueDate && (
                <div className={`arm-due-banner ${isOverdue ? 'arm-due-banner--overdue' : ''}`}>
                  <Clock size={16} />
                  <span>
                    {isOverdue
                      ? `Vencido desde ${dueDate}`
                      : `Fecha límite: ${dueDate}`}
                  </span>
                </div>
              )}

              <div className="arm-project-list">
                {pendingProjects.map((project) => (
                  <div key={project.projectId} className="arm-project-row">
                    <div className="arm-project-row__icon">
                      <FileText size={16} />
                    </div>
                    <div className="arm-project-row__info">
                      <strong>{project.projectName || project.projectId}</strong>
                      <span>{project.periodo}</span>
                    </div>
                    <button
                      className="arm-project-row__btn"
                      onClick={() => handleNavigate(project.projectId)}
                    >
                      <ExternalLink size={13} />
                      Cargar ahora
                    </button>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        <div className="arm-footer">
          <button className="arm-btn-remind" onClick={onClose}>
            Recordar luego
          </button>
          {pendingProjects.length > 0 && (
            <button
              className="arm-btn-all"
              onClick={() => handleNavigate(pendingProjects[0].projectId)}
            >
              <FileText size={14} />
              Ver proyectos
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdvanceReportLoginModal;
