import React from 'react';
import { Check } from 'lucide-react';
import './Stepper.css';

const STEPS = [
  { num: 1, label: 'Datos Generales' },
  { num: 2, label: 'Patrocinador\ny Equipo' },
  { num: 3, label: 'Fases, Hitos\ny Entregables' },
  { num: 4, label: 'PETI y\nComunicaciones' },
  { num: 5, label: 'Cuestionario\nFURAG' },
  { num: 6, label: 'Matriz de\nRiesgos' },
  { num: 7, label: 'Gestión\nDocumental' },
];

const Stepper = ({ currentStep }) => {
  return (
    <div className="stepper-container">
      {STEPS.map((step, index) => {
        const isCompleted = currentStep > step.num;
        const isActive = currentStep === step.num;

        return (
          <React.Fragment key={step.num}>
            <div className={`stepper-step ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}>
              <div className="stepper-circle">
                {isCompleted ? <Check size={16} /> : step.num}
              </div>
              <div className="stepper-label">
                {step.label.split('\n').map((line, i) => (
                  <span key={i}>{line}<br /></span>
                ))}
              </div>
            </div>
            {index < STEPS.length - 1 && (
              <div className={`stepper-line ${isCompleted ? 'completed' : ''}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

export default Stepper;
