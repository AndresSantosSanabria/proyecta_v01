import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Lock, Info } from 'lucide-react';
import projectService from '../../services/projectService';
import ProgressHeader from '../../components/features/progress/ProgressHeader';
import ProgressKPIs from '../../components/features/progress/ProgressKPIs';
import ProgressTreeTable from '../../components/features/progress/ProgressTreeTable';
import './ProjectProgressPage.css';

const ProjectProgressPage = () => {
  const params = useParams();
  const codigoProyecto = (params.codigoProyecto || params.id || '').toUpperCase();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [expandedNodes, setExpandedNodes] = useState({});

  const toggleNode = (nodeId) => {
    setExpandedNodes(prev => ({
      ...prev,
      [nodeId]: prev[nodeId] === undefined ? false : !prev[nodeId]
    }));
  };

  const isExpanded = (nodeId) => {
    return expandedNodes[nodeId] !== false;
  };

  const [progressData, setProgressData] = useState({
    codigo: codigoProyecto,
    nombre: 'Cargando proyecto...',
    avanceTotal: 0,
    entregablesConformidad: 0,
    entregablesTotal: 0,
    atrasados: 0,
    proximosVencer: 0,
    fases: []
  });

  const fetchProgress = useCallback(async () => {
    try {
      setLoading(true);
      const response = await projectService.getProgress(codigoProyecto);
      const apiData = response.data || response;

      const hasFases = apiData.fases && apiData.fases.length > 0;
      setProgressData({
        codigo: apiData.codigo || apiData.proyectoId || codigoProyecto,
        nombre: apiData.nombre || apiData.nombreProyecto || 'Proyecto',
        avanceTotal: hasFases ? parseFloat(apiData.avanceTotal || apiData.avance || 0) : 0,
        entregablesConformidad: hasFases ? (apiData.entregablesConformidad || apiData.entregablesConformes || 0) : 0,
        entregablesTotal: hasFases ? (apiData.entregablesTotal || 0) : 0,
        atrasados: hasFases ? (apiData.atrasados || apiData.entregablesAtrasados || 0) : 0,
        proximosVencer: hasFases ? (apiData.proximosVencer || 0) : 0,
        fases: hasFases ? apiData.fases : []
      });
      setError(null);
    } catch (err) {
      console.error('Error fetching project progress:', err);
      setError('No se pudo cargar la información del proyecto.');
    } finally {
      setLoading(false);
    }
  }, [codigoProyecto]);

  useEffect(() => {
    fetchProgress();
  }, [fetchProgress, refreshKey]);

  const handleEvidenceUploaded = () => {
    setRefreshKey(prev => prev + 1);
  };

  if (loading) {
    return <div className="progress-page-container"><h2>Cargando avance...</h2></div>;
  }

  return (
    <div className="progress-page-container">
      <ProgressHeader codigo={progressData.codigo} />

      <ProgressKPIs progressData={progressData} />

      <div className="info-alert">
        <Info size={18} className="info-icon" />
        <p>Los campos marcados con <Lock size={14} className="inline-icon" /> son <strong>calculados automáticamente</strong> por el sistema. Solo puedes marcar entregables como "A conformidad" y subir el PDF de evidencia.</p>
      </div>

      {error && (
        <div className="error-banner" style={{ padding: '1rem', background: '#ffebee', borderRadius: '8px', marginBottom: '1rem', color: '#c62828' }}>
          {error}
          <button onClick={fetchProgress} style={{ marginLeft: '1rem', padding: '0.3rem 0.8rem', border: '1px solid #c62828', background: 'transparent', borderRadius: '4px', cursor: 'pointer', color: '#c62828' }}>Reintentar</button>
        </div>
      )}

      <ProgressTreeTable
        progressData={progressData}
        isExpanded={isExpanded}
        toggleNode={toggleNode}
        proyectoId={codigoProyecto}
        onEvidenceUploaded={handleEvidenceUploaded}
      />
    </div>
  );
};

export default ProjectProgressPage;
