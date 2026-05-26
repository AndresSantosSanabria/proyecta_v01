import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { Lock, Info } from 'lucide-react';
import projectService from '../../services/projectService';
import ProgressHeader from '../../components/features/progress/ProgressHeader';
import ProgressKPIs from '../../components/features/progress/ProgressKPIs';
import ProgressTreeTable from '../../components/features/progress/ProgressTreeTable';
import './ProjectProgressPage.css';

const normalizeProgressPayload = (payload, fallbackCode) => {
  const data = payload?.data ?? payload ?? {};
  return {
    codigo: data.codigo || data.proyectoId || fallbackCode,
    nombre: data.nombre || data.nombreProyecto || 'Proyecto',
    dependencia: data.dependencia || data.dependenciaNombre || '',
    progresoProgramado: data.progresoProgramado ?? data.avanceProgramado ?? 0,
    progresoEjecutado: data.progresoEjecutado ?? data.avanceTotal ?? 0,
    diferencia: data.diferencia ?? 0,
    eficacia: data.eficacia ?? 1,
    estado: data.estado || 'EN_TIEMPO',
    avanceTotal: data.avanceTotal ?? data.progresoEjecutado ?? 0,
    entregablesConformidad: data.entregablesConformidad ?? data.entregablesConformes ?? 0,
    entregablesConformes: data.entregablesConformes ?? data.entregablesConformidad ?? 0,
    entregablesTotal: data.entregablesTotal ?? 0,
    atrasados: data.entregablesAtrasados ?? data.atrasados ?? 0,
    proximosVencer: data.proximosAVencer ?? data.proximosVencer ?? 0,
    corte: data.corte || new Date().toISOString().slice(0, 10),
    fases: Array.isArray(data.fases) ? data.fases : [],
  };
};

const toNumber = (value) => {
  if (value == null) return 0;
  const parsed = typeof value === 'string' ? parseFloat(value) : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const flattenEntregables = (fases = []) => {
  return fases.flatMap((fase) =>
    (fase.hitos || []).flatMap((hito) =>
      (hito.entregables || []).map((entregable) => ({
        ...entregable,
        faseNombre: fase.nombre,
        hitoNombre: hito.nombre,
      }))
    )
  );
};

const ProjectProgressPage = () => {
  const params = useParams();
  const codigoProyecto = (params.codigoProyecto || params.id || '').toUpperCase();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [expandedNodes, setExpandedNodes] = useState({});
  const [projectInfo, setProjectInfo] = useState(null);
  const [progressData, setProgressData] = useState({
    codigo: codigoProyecto,
    nombre: 'Cargando proyecto...',
    dependencia: '',
    progresoProgramado: 0,
    progresoEjecutado: 0,
    diferencia: 0,
    eficacia: 1,
    estado: 'EN_TIEMPO',
    avanceTotal: 0,
    entregablesConformidad: 0,
    entregablesConformes: 0,
    entregablesTotal: 0,
    atrasados: 0,
    proximosVencer: 0,
    fases: [],
  });

  const toggleNode = (nodeId) => {
    setExpandedNodes(prev => ({
      ...prev,
      [nodeId]: prev[nodeId] === undefined ? false : !prev[nodeId]
    }));
  };

  const isExpanded = (nodeId) => expandedNodes[nodeId] !== false;

  const fetchProgress = useCallback(async () => {
    try {
      setLoading(true);
      const [progressResponse, projectResponse] = await Promise.all([
        projectService.getProgress(codigoProyecto),
        projectService.getById(codigoProyecto),
      ]);
      setProgressData(normalizeProgressPayload(progressResponse?.data, codigoProyecto));
      const projectData = projectResponse?.data?.data ?? projectResponse?.data ?? null;
      setProjectInfo(projectData);
      setProgressData((current) => ({
        ...current,
        dependencia: projectData?.dependencia || current.dependencia || '',
      }));
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

  const resumenExcel = useMemo(() => {
    const entregables = flattenEntregables(progressData.fases || []);
    const corte = progressData.corte ? new Date(progressData.corte) : new Date();

    const programadosAlCorte = entregables.filter((ent) => {
      if (!ent.fechaLimite) return false;
      return new Date(ent.fechaLimite) <= corte;
    }).length;

    const entregadosAlCorte = entregables.filter((ent) => Boolean(ent.fechaEntrega || ent.fechaEntregaReal || ent.estadoCodigo === 'COMPLETADO' || ent.estadoCodigo === 'A_CONFORMIDAD' || ent.estado === 'A_CONFORMIDAD')).length;

    const entregadosATiempo = entregables.filter((ent) => {
      const fechaEntrega = ent.fechaEntrega || ent.fechaEntregaReal;
      if (!fechaEntrega || !ent.fechaLimite) return false;
      return new Date(fechaEntrega) <= new Date(ent.fechaLimite);
    }).length;

    const eficacia = programadosAlCorte > 0 ? (entregadosAlCorte / programadosAlCorte) * 100 : 100;
    const eficiencia = entregadosAlCorte > 0 ? (entregadosATiempo / entregadosAlCorte) * 100 : 100;

    return {
      meta: projectInfo?.objetivoGeneral || projectInfo?.meta || progressData.nombre,
      dependencia: projectInfo?.dependencia || progressData.dependencia || 'Sin dependencia',
      programado: toNumber(progressData.progresoProgramado),
      avance: toNumber(progressData.progresoEjecutado),
      diferencia: toNumber(progressData.diferencia),
      estado: progressData.estado || 'EN_TIEMPO',
      totalEntregables: toNumber(progressData.entregablesTotal),
      programadosAlCorte,
      entregadosAlCorte,
      entregadosATiempo,
      eficacia,
      eficiencia,
    };
  }, [progressData, projectInfo]);

  if (loading) {
    return <div className="progress-page-container"><h2>Cargando avance...</h2></div>;
  }

  return (
    <div className="progress-page-container">
      <ProgressHeader codigo={progressData.codigo} />

      <section className="excel-summary-card">
        <div className="excel-summary-top">
          <div className="excel-summary-title">
            <span className="excel-kicker">INDICADORES AL CORTE</span>
            <h2>{progressData.codigo} · {progressData.nombre}</h2>
            <p>{resumenExcel.meta}</p>
          </div>
          <div className={`excel-state ${resumenExcel.estado === 'ATRASO' ? 'danger' : 'success'}`}>
            {resumenExcel.estado}
          </div>
        </div>

        <div className="excel-summary-grid">
          <article>
            <span>Meta</span>
            <strong>{resumenExcel.meta}</strong>
          </article>
          <article>
            <span>Dependencia</span>
            <strong>{resumenExcel.dependencia}</strong>
          </article>
          <article>
            <span>Programado</span>
            <strong>{resumenExcel.programado.toFixed(0)}%</strong>
          </article>
          <article>
            <span>Avance</span>
            <strong>{resumenExcel.avance.toFixed(0)}%</strong>
          </article>
          <article>
            <span>Diferencia</span>
            <strong>{resumenExcel.diferencia.toFixed(0)}%</strong>
          </article>
          <article>
            <span>Total entregables</span>
            <strong>{resumenExcel.totalEntregables}</strong>
          </article>
          <article>
            <span>Programados al corte</span>
            <strong>{resumenExcel.programadosAlCorte}</strong>
          </article>
          <article>
            <span>Entregados al corte</span>
            <strong>{resumenExcel.entregadosAlCorte}</strong>
          </article>
          <article>
            <span>Eficacia</span>
            <strong>{resumenExcel.eficacia.toFixed(1)}%</strong>
          </article>
          <article>
            <span>Eficiencia</span>
            <strong>{resumenExcel.eficiencia.toFixed(1)}%</strong>
          </article>
        </div>
      </section>

      <ProgressKPIs progressData={progressData} />

      <div className="info-alert">
        <Info size={18} className="info-icon" />
        <p>
          Los campos marcados con <Lock size={14} className="inline-icon" /> son calculados automáticamente por el sistema.
          El semáforo del entregable se basa en los días devueltos por el backend.
        </p>
      </div>

      {error && (
        <div className="error-banner" style={{ padding: '1rem', background: '#ffebee', borderRadius: '8px', marginBottom: '1rem', color: '#c62828' }}>
          {error}
          <button onClick={fetchProgress} style={{ marginLeft: '1rem', padding: '0.3rem 0.8rem', border: '1px solid #c62828', background: 'transparent', borderRadius: '4px', cursor: 'pointer', color: '#c62828' }}>Reintentar</button>
        </div>
      )}

      <ProgressTreeTable
        progressData={progressData}
        excelSummary={resumenExcel}
        isExpanded={isExpanded}
        toggleNode={toggleNode}
        proyectoId={codigoProyecto}
        onEvidenceUploaded={handleEvidenceUploaded}
      />
    </div>
  );
};

export default ProjectProgressPage;
