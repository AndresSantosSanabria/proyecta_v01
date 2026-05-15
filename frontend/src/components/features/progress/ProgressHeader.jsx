import React from 'react';

const ProgressHeader = ({ codigo }) => {
  return (
    <header className="progress-header">
      <div className="header-title">
        <h1>Avance del Proyecto</h1>
        <span className="project-id-badge">— {codigo}</span>
      </div>
    </header>
  );
};

export default ProgressHeader;
