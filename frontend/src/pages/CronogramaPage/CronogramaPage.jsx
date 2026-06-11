import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Loader2, CalendarRange } from 'lucide-react';
import cronogramaService from '../../services/cronogramaService';
import DocumentUpload from '../../components/common/DocumentUpload';
import CronogramaHeader from '../../components/features/cronograma/CronogramaHeader';
import ProjectInfoCard from '../../components/features/cronograma/ProjectInfoCard';
import GanttChart from '../../components/features/cronograma/GanttChart';
import './CronogramaPage.css';

const safeDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const toIsoDate = (date) => {
  if (!date) return null;
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  const day = String(date.getUTCDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const monthSpan = (start, end) => {
  if (!start || !end) return 1;
  const diff = ((end.getUTCFullYear() - start.getUTCFullYear()) * 12)
    + (end.getUTCMonth() - start.getUTCMonth()) + 1;
  return Math.max(1, diff);
};

const minDate = (dates) => dates
  .filter(Boolean)
  .reduce((min, date) => (!min || date.getTime() < min.getTime() ? date : min), null);

const maxDate = (dates) => dates
  .filter(Boolean)
  .reduce((max, date) => (!max || date.getTime() > max.getTime() ? date : max), null);

const CronogramaPage = () => {
  const { id: rawProyectoId } = useParams();
  const proyectoId = String(rawProyectoId || '').trim().toUpperCase();
  const queryClient = useQueryClient();
  const [summaryExpanded, setSummaryExpanded] = useState(false);

  const formatSummaryValue = (value, fallback = 'Sin dato') => {
    if (value === null || value === undefined || value === '') return fallback;
    if (typeof value === 'number') {
      return Number.isFinite(value) ? value.toLocaleString('es-CO') : fallback;
    }
    return String(value);
  };

  const {
    data: cronogramaData,
    isLoading: isLoadingCronograma,
    error: cronogramaError,
  } = useQuery({
    queryKey: ['cronograma', proyectoId],
    queryFn: () => cronogramaService.getCronograma(proyectoId),
  });

  const {
    data: resumenData,
    isLoading: isLoadingResumen,
  } = useQuery({
    queryKey: ['resumen', proyectoId],
    queryFn: () => cronogramaService.getResumenProyecto(proyectoId),
  });

  const handleCronogramaUpload = () => {
    queryClient.invalidateQueries({ queryKey: ['cronograma', proyectoId] });
    queryClient.invalidateQueries({ queryKey: ['resumen', proyectoId] });
  };

  if (isLoadingCronograma || isLoadingResumen) {
    return (
      <div className="cronograma-container">
        <div className="loading-overlay">
          <Loader2 className="animate-spin" size={48} />
          <p>Cargando información del cronograma...</p>
        </div>
      </div>
    );
  }

  const hasRealData = Boolean(cronogramaData?.data?.vistaGantt?.length);
  const resumenApi = resumenData?.data?.data ?? resumenData?.data ?? {};

  const displayResumen = {
    fechaInicio: resumenApi.fechaInicio || 'Sin fecha',
    director: resumenApi.director || '',
    fases: resumenApi.totalFases || 0,
    totalHitos: resumenApi.totalHitos || 0,
    avance: resumenApi.avance_total ? `${Number(resumenApi.avance_total).toFixed(0)}%` : `${Number(resumenApi.avanceTotal || 0).toFixed(0)}%`,
    meta: formatSummaryValue(
      resumenApi.meta
      || resumenApi.objetivoGeneral
      || resumenApi.objetivo_general
      || resumenApi.descripcion
      || resumenApi.nombre
      || 'Sin meta definida',
    ),
    dependencia: formatSummaryValue(
      resumenApi.dependencia
      || resumenApi.dependenciaResponsable
      || resumenApi.dependencia_responsable
      || resumenApi.area
      || 'No asignada',
    ),
    programado: formatSummaryValue(
      resumenApi.programado_total
      || resumenApi.programado
      || resumenApi.programadoTotal
      || resumenApi.totalProgramado
      || resumenApi.porcentajeProgramado
      || '0',
    ),
    programadosAlCorte: formatSummaryValue(
      resumenApi.programados_al_corte
      || resumenApi.programadosAlCorte
      || resumenApi.programados
      || 0,
    ),
    entregadosAlCorte: formatSummaryValue(
      resumenApi.entregados_al_corte
      || resumenApi.entregadosAlCorte
      || resumenApi.entregados
      || 0,
    ),
    eficacia: resumenApi.eficacia
      ?? resumenApi.eficaciaTotal
      ?? resumenApi.eficacia_total
      ?? 100,
    eficiencia: resumenApi.eficiencia
      ?? resumenApi.eficienciaTotal
      ?? resumenApi.eficiencia_total
      ?? 100,
  };

  const displayCronograma = hasRealData ? cronogramaData.data.vistaGantt.map((fase) => {
    const hitosMapped = fase.hitos?.map((h) => {
      const start = safeDate(h.fechaInicio);
      const end = safeDate(h.fechaFin) || start;
      const mesInicio = start ? start.getUTCMonth() : 0;
      const duracion = monthSpan(start, end);
      const entregablesMapped = (h.entregables || []).map((entregable) => {
        const entregableStart = safeDate(entregable.fechaInicio) || safeDate(entregable.fechaFin);
        const entregableEnd = safeDate(entregable.fechaFin) || entregableStart;

        return {
          id: `e${entregable.entregableId}`,
          nombre: entregable.nombre,
          mesInicio: entregableStart ? entregableStart.getUTCMonth() : 0,
          duracionMeses: monthSpan(entregableStart, entregableEnd),
          avance: entregable.avance || 0,
          ponderacion: entregable.ponderacion || 0,
          estado: entregable.estado || '',
          fechaInicio: entregable.fechaInicio,
          fechaFin: entregable.fechaFin,
        };
      }).sort((a, b) => {
        const left = safeDate(a.fechaInicio);
        const right = safeDate(b.fechaInicio);
        if (left && right) return left.getTime() - right.getTime();
        return a.mesInicio - b.mesInicio;
      });

      return {
        id: `h${h.hitoId}`,
        nombre: h.nombre,
        mesInicio,
        duracionMeses: Math.max(0.5, duracion),
        avance: h.avance || 0,
        fechaInicio: h.fechaInicio,
        fechaFin: h.fechaFin,
        entregables: entregablesMapped,
      };
    }).sort((a, b) => {
      const left = safeDate(a.fechaInicio);
      const right = safeDate(b.fechaInicio);
      if (left && right) return left.getTime() - right.getTime();
      return a.mesInicio - b.mesInicio;
    }) || [];

    const faseInicio = minDate(hitosMapped.map((h) => safeDate(h.fechaInicio)));
    const faseFin = maxDate(hitosMapped.map((h) => safeDate(h.fechaFin) || safeDate(h.fechaInicio)));

    const mesInicioFase = hitosMapped.length > 0
      ? Math.min(...hitosMapped.map((h) => h.mesInicio))
      : 0;

    const mesFinFase = hitosMapped.length > 0
      ? Math.max(...hitosMapped.map((h) => h.mesInicio + h.duracionMeses))
      : 1;

    return {
      id: `f${fase.faseId}`,
      nombre: fase.nombre,
      mesInicio: mesInicioFase,
      duracionMeses: faseInicio && faseFin ? monthSpan(faseInicio, faseFin) : Math.max(1, mesFinFase - mesInicioFase),
      avance: fase.avance || 0,
      fechaInicio: toIsoDate(faseInicio),
      fechaFin: toIsoDate(faseFin),
      hitos: hitosMapped,
    };
  }).sort((a, b) => {
    const left = safeDate(a.fechaInicio);
    const right = safeDate(b.fechaInicio);
    if (left && right) return left.getTime() - right.getTime();
    return a.mesInicio - b.mesInicio;
  }) : [];

  const parseYear = (value) => {
    const date = value ? new Date(value) : null;
    return date && !Number.isNaN(date.getTime()) ? date.getUTCFullYear() : new Date().getUTCFullYear();
  };
  const ganttYear = parseYear(displayResumen.fechaInicio);

  return (
    <div className="cronograma-container">
      <CronogramaHeader proyectoId={proyectoId} />

      {cronogramaError && (
        <div className="error-container">
          <AlertCircle size={20} />
          <span>No se pudo cargar el cronograma real. Verifica que el proyecto tenga fases e hitos configurados.</span>
        </div>
      )}

      <div className="top-cards-grid">
        <DocumentUpload
          proyectoId={proyectoId}
          tipoDocumento="CRONOGRAMA"
          label="Cronograma del Proyecto"
          onUploadSuccess={handleCronogramaUpload}
        />
        <ProjectInfoCard
          displayResumen={displayResumen}
          expanded={summaryExpanded}
          onToggleExpanded={() => setSummaryExpanded((prev) => !prev)}
        />
      </div>

      {hasRealData ? (
        <GanttChart displayCronograma={displayCronograma} year={ganttYear} />
      ) : (
        <div className="visual-cronograma-section empty-state">
          <div className="section-header">
            <h2><CalendarRange size={18} /> Sin líneas de tiempo registradas</h2>
          </div>
          <div className="empty-cronograma">
            No hay fases ni hitos cargados para construir una vista de cronograma real.
          </div>
        </div>
      )}
    </div>
  );
};

export default CronogramaPage;
