import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Info, Lock, Maximize2, X, FileText, Eye } from 'lucide-react';
import projectService from '../../services/projectService';
import { usePermission } from '../../hooks/usePermission';
import ProgressHeader from '../../components/features/progress/ProgressHeader';
import ProgressKPIs from '../../components/features/progress/ProgressKPIs';
import ProgressTreeTable from '../../components/features/progress/ProgressTreeTable';
import ProjectBenefitImpactPanel from '../../components/projects/ProjectBenefitImpactPanel';
import BenefitImpactReviewModal from '../../components/projects/BenefitImpactReviewModal';
import ProjectInfoModal from '../../components/projects/ProjectInfoModal';
import { formatDate } from '../../utils/locale';
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
    eficiencia: data.eficiencia ?? 1,
    estado: data.estado || 'EN_TIEMPO',
    avanceTotal: data.avanceTotal ?? data.progresoEjecutado ?? 0,
    entregablesConformidad: data.entregablesConformidad ?? data.entregablesConformes ?? 0,
    entregablesConformes: data.entregablesConformes ?? data.entregablesConformidad ?? 0,
    entregablesTotal: data.entregablesTotal ?? 0,
    entregablesAtrasados: data.entregablesAtrasados ?? data.atrasados ?? 0,
    atrasados: data.entregablesAtrasados ?? data.atrasados ?? 0,
    proximosVencer: data.proximosAVencer ?? data.proximosVencer ?? 0,
    entregablesProgramadosAlCorte: data.entregablesProgramadosAlCorte ?? data.programadosAlCorte ?? 0,
    entregablesEntregadosAlCorte: data.entregablesEntregadosAlCorte ?? data.entregadosAlCorte ?? 0,
    entregablesEntregadosATiempo: data.entregablesEntregadosATiempo ?? data.entregadosATiempo ?? 0,
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
    eficiencia: 1,
    estado: 'EN_TIEMPO',
    avanceTotal: 0,
    entregablesConformidad: 0,
    entregablesConformes: 0,
    entregablesTotal: 0,
    entregablesAtrasados: 0,
    atrasados: 0,
    proximosVencer: 0,
    entregablesProgramadosAlCorte: 0,
    entregablesEntregadosAlCorte: 0,
    entregablesEntregadosATiempo: 0,
    fases: [],
  });
  const [summaryModalOpen, setSummaryModalOpen] = useState(false);
  const [projectInfoModalOpen, setProjectInfoModalOpen] = useState(false);
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [benefitImpactData, setBenefitImpactData] = useState(null);

  const showBenefitImpact = useMemo(() => {
    const allEntregables = flattenEntregables(progressData.fases || []);
    if (allEntregables.length === 0) return false;
    const isAprobado = (ent) => {
      const estado = String(ent.estadoCodigo || ent.estado || '').toUpperCase();
      return estado === 'A_CONFORMIDAD'
        || estado === 'APROBADO'
        || ent.conforme === true;
    };
    const tieneEvidencia = (ent) => Boolean(ent.fechaEntrega || ent.fechaEntregaReal || ent.evidenciaUrl || ent.descargaUrl);
    return allEntregables.every((ent) => tieneEvidencia(ent) && isAprobado(ent));
  }, [progressData.fases]);

  const canApproveBenefits = usePermission('BENEFICIO:APROBAR');
  const canViewBenefits = usePermission('BENEFICIO:VER');
  const hasBenefitData = benefitImpactData && ['DILIGENCIADO', 'OBSERVADO', 'RECHAZADO', 'APROBADO'].includes(benefitImpactData.estado);
  const canReview = canApproveBenefits && hasBenefitData;
  const canViewBenefitModal = hasBenefitData && (canApproveBenefits || canViewBenefits);

  useEffect(() => {
    if (!codigoProyecto) return;
    let active = true;
    const fetchBenefitImpact = async () => {
      try {
        const response = await projectService.getBenefitImpact(codigoProyecto);
        const payload = response?.data?.data ?? response?.data ?? response ?? null;
        if (active) setBenefitImpactData(payload);
      } catch {
        if (active) setBenefitImpactData(null);
      }
    };
    fetchBenefitImpact();
    return () => { active = false; };
  }, [codigoProyecto, refreshKey]);

  const toggleNode = (nodeId) => {
    setExpandedNodes((prev) => ({
      ...prev,
      [nodeId]: prev[nodeId] === undefined ? false : !prev[nodeId],
    }));
  };

  const isExpanded = (nodeId) => expandedNodes[nodeId] !== false;

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      try {
        setLoading(true);
        const [progressResponse, projectResponse] = await Promise.all([
          projectService.getProgress(codigoProyecto),
          projectService.getById(codigoProyecto),
        ]);

        if (cancelled) return;

        setProgressData(normalizeProgressPayload(progressResponse?.data, codigoProyecto));
        const projectData = projectResponse?.data?.data ?? projectResponse?.data ?? null;
        setProjectInfo(projectData);
        setProgressData((current) => ({
          ...current,
          dependencia: projectData?.dependencia || current.dependencia || '',
        }));
        setError(null);
      } catch (err) {
        if (cancelled) return;
        console.error('Error fetching project progress:', err);
        setError('No se pudo cargar la información del proyecto.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void run();

    return () => {
      cancelled = true;
    };
  }, [codigoProyecto, refreshKey]);

  useEffect(() => {
    if (!summaryModalOpen) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setSummaryModalOpen(false);
      }
    };

    document.body.classList.add('modal-open');
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.classList.remove('modal-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [summaryModalOpen]);

  const handleEvidenceUploaded = () => {
    setRefreshKey((prev) => prev + 1);
  };

  const resumenExcel = useMemo(() => {
    const entregables = flattenEntregables(progressData.fases || []);
    const corte = progressData.corte ? new Date(progressData.corte) : new Date();

    const programadosAlCorte = progressData.entregablesProgramadosAlCorte
      ?? entregables.filter((ent) => ent.fechaLimite && new Date(ent.fechaLimite) <= corte).length;

    const entregadosAlCorte = progressData.entregablesEntregadosAlCorte
      ?? entregables.filter((ent) => {
        if (!ent.fechaLimite || new Date(ent.fechaLimite) > corte) return false;
        return Boolean(
          ent.fechaEntrega
          || ent.fechaEntregaReal
          || ent.estadoCodigo === 'COMPLETADO'
          || ent.estadoCodigo === 'A_CONFORMIDAD'
          || ent.estado === 'A_CONFORMIDAD',
        );
      }).length;

    const entregadosATiempo = progressData.entregablesEntregadosATiempo
      ?? entregables.filter((ent) => {
        const fechaLimite = ent.fechaLimite ? new Date(ent.fechaLimite) : null;
        const fechaEntrega = ent.fechaEntrega || ent.fechaEntregaReal;
        if (!fechaLimite || !fechaEntrega) return false;
        if (fechaLimite > corte) return false;
        const entrega = new Date(fechaEntrega);
        return entrega <= fechaLimite;
      }).length;

    const eficacia = progressData.eficacia != null
      ? Number(progressData.eficacia) * (Number(progressData.eficacia) <= 1 ? 100 : 1)
      : (programadosAlCorte > 0 ? (entregadosAlCorte / programadosAlCorte) * 100 : 0);
    const eficiencia = progressData.eficiencia != null
      ? Number(progressData.eficiencia) * (Number(progressData.eficiencia) <= 1 ? 100 : 1)
      : (entregadosAlCorte > 0 ? (entregadosATiempo / entregadosAlCorte) * 100 : 0);

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

  const metaPreview = useMemo(() => {
    const text = String(resumenExcel.meta || '');
    if (text.length <= 140) return text;
    return `${text.slice(0, 140).trim()}…`;
  }, [resumenExcel.meta]);

  if (loading) {
    return <div className="progress-page-container"><h2>Cargando avance...</h2></div>;
  }

  return (
    <div className="progress-page-container">
      <ProgressHeader
        codigo={progressData.codigo}
        nombre={progressData.nombre}
        dependencia={progressData.dependencia || projectInfo?.dependencia}
        estado={progressData.estado}
        corte={formatDate(progressData.corte) || formatDate(new Date())}
      />

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
        {canViewBenefitModal && (
          <button
            type="button"
            className="pim-trigger"
            onClick={() => setReviewModalOpen(true)}
          >
            <Eye size={16} />
            {canApproveBenefits ? 'Revisar Beneficios e Impacto' : 'Ver Beneficios e Impacto'}
          </button>
        )}
        <button
          type="button"
          className="pim-trigger"
          onClick={() => setProjectInfoModalOpen(true)}
        >
          <FileText size={16} />
          Informacion del proyecto
        </button>
      </div>

      <section className="excel-summary-card">
        <div className="excel-summary-top">
          <div className="excel-summary-title">
            <span className="excel-kicker">Indicadores al corte</span>
            <h2 className="summary-clamp-two">{progressData.codigo} · {progressData.nombre}</h2>
            <p className="summary-clamp-two">{metaPreview}</p>
          </div>
          <div className="excel-summary-actions">
            <div className={`excel-state ${resumenExcel.estado === 'ATRASO' ? 'danger' : 'success'}`}>
              {resumenExcel.estado}
            </div>
            <button
              type="button"
              className="excel-summary-open"
              onClick={() => setSummaryModalOpen(true)}
            >
              <Maximize2 size={16} />
              Ver detalle
            </button>
          </div>
        </div>

        <div className="excel-summary-grid">
          <article>
            <span>Meta</span>
            <strong className="summary-clamp-two">{resumenExcel.meta}</strong>
          </article>
          <article>
            <span>Dependencia</span>
            <strong className="summary-clamp-two">{resumenExcel.dependencia}</strong>
          </article>
          <article>
            <span>Fecha límite</span>
            <strong>{formatDate(progressData.corte) || 'Sin fecha'}</strong>
          </article>
          <article>
            <span>Estado</span>
            <strong>{resumenExcel.estado}</strong>
          </article>
        </div>
      </section>

      {summaryModalOpen && (
        <div
          className="project-summary-modal-overlay"
          role="presentation"
          onClick={() => setSummaryModalOpen(false)}
        >
          <div
            className="project-summary-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="project-summary-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="project-summary-modal-header">
              <div>
<span className="excel-kicker">Indicadores a fecha límite</span>
                <h3 id="project-summary-modal-title">{progressData.codigo} · {progressData.nombre}</h3>
                <p>Detalle completo a fecha límite con los indicadores calculados por el sistema.</p>
              </div>
              <button
                type="button"
                className="project-summary-modal-close"
                onClick={() => setSummaryModalOpen(false)}
                aria-label="Cerrar detalle"
              >
                <X size={18} />
              </button>
            </div>

            <div className="project-summary-modal-body">
              <section className="project-summary-modal-block full-width">
                <span>Meta</span>
                <p>{resumenExcel.meta}</p>
              </section>
              <section className="project-summary-modal-block full-width">
                <span>Dependencia</span>
                <p>{resumenExcel.dependencia}</p>
              </section>
              <section className="project-summary-modal-block">
                <span>Programado</span>
                <strong>{resumenExcel.programado.toFixed(0)}%</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Avance</span>
                <strong>{resumenExcel.avance.toFixed(0)}%</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Diferencia</span>
                <strong>{resumenExcel.diferencia.toFixed(0)}%</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Total entregables</span>
                <strong>{resumenExcel.totalEntregables}</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Programados a fecha límite</span>
                <strong>{resumenExcel.programadosAlCorte}</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Entregados a fecha límite</span>
                <strong>{resumenExcel.entregadosAlCorte}</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Eficacia</span>
                <strong>{resumenExcel.eficacia.toFixed(1)}%</strong>
              </section>
              <section className="project-summary-modal-block">
                <span>Eficiencia</span>
                <strong>{resumenExcel.eficiencia.toFixed(1)}%</strong>
              </section>
            </div>
          </div>
        </div>
      )}

      <ProgressKPIs progressData={progressData} />

      {showBenefitImpact && !canApproveBenefits && canViewBenefits && (
        <ProjectBenefitImpactPanel
          proyectoId={codigoProyecto}
          projectName={projectInfo?.nombre || progressData.nombre}
          refreshToken={refreshKey}
          onSaved={() => setRefreshKey((prev) => prev + 1)}
        />
      )}

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
          <button onClick={() => setRefreshKey((prev) => prev + 1)} style={{ marginLeft: '1rem', padding: '0.3rem 0.8rem', border: '1px solid #c62828', background: 'transparent', borderRadius: '4px', cursor: 'pointer', color: '#c62828' }}>Reintentar</button>
        </div>
      )}

      <ProgressTreeTable
        progressData={progressData}
        projectInfo={projectInfo}
        excelSummary={resumenExcel}
        isExpanded={isExpanded}
        toggleNode={toggleNode}
        proyectoId={codigoProyecto}
        onEvidenceUploaded={handleEvidenceUploaded}
      />

      <ProjectInfoModal
        project={projectInfo}
        open={projectInfoModalOpen}
        onClose={() => setProjectInfoModalOpen(false)}
        onDocumentUploaded={() => setRefreshKey((prev) => prev + 1)}
      />

      <BenefitImpactReviewModal
        isOpen={reviewModalOpen}
        onClose={() => setReviewModalOpen(false)}
        proyectoId={codigoProyecto}
        projectName={projectInfo?.nombre || progressData.nombre}
        benefitImpactData={benefitImpactData}
        onSuccess={() => setRefreshKey((prev) => prev + 1)}
        isDirectorOnly={!canApproveBenefits && canViewBenefits}
      />
    </div>
  );
};

export default ProjectProgressPage;
