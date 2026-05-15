import React from 'react';
import { Info } from 'lucide-react';

const ProjectInfoCard = ({ displayResumen }) => {
  return (
    <div className="cronograma-card">
      <h3 className="card-title">
        <Info size={18} />
        Información del Proyecto
      </h3>
      <div className="info-list">
        <div className="info-item">
          <span className="info-label">Fecha inicio:</span>
          <span className="info-value">{displayResumen.fechaInicio}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Director:</span>
          <span className="info-value">{displayResumen.director}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Fases:</span>
          <span className="info-value">{displayResumen.fases}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Total hitos:</span>
          <span className="info-value">{displayResumen.totalHitos}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Avance:</span>
          <span className="info-value status-active">{displayResumen.avance}</span>
        </div>
      </div>
    </div>
  );
};

export default ProjectInfoCard;
