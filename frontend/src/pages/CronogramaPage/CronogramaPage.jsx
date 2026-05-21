import React from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Loader2 } from 'lucide-react';
import cronogramaService from '../../services/cronogramaService';
import DocumentUpload from '../../components/common/DocumentUpload';
import CronogramaHeader from '../../components/features/cronograma/CronogramaHeader';
import ProjectInfoCard from '../../components/features/cronograma/ProjectInfoCard';
import GanttChart from '../../components/features/cronograma/GanttChart';
import './CronogramaPage.css';

const CronogramaPage = () => {
  const { id: proyectoId } = useParams();
  const queryClient = useQueryClient();

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

  const hasRealData = cronogramaData?.data?.vistaGantt && cronogramaData.data.vistaGantt.length > 0;

  const displayResumen = (resumenData?.data && hasRealData) ? {
    fechaInicio: resumenData.data.fechaInicio || 'Pendiente',
    director: resumenData.data.director || 'No asignado',
    fases: resumenData.data.totalFases || 0,
    totalHitos: resumenData.data.totalHitos || 0,
    avance: `${(resumenData.data.avanceTotal || 0).toFixed(0)}%`,
  } : {
    fechaInicio: resumenData?.data?.fechaInicio || '2024-03-15',
    director: resumenData?.data?.director || 'No asignado',
    fases: 2,
    totalHitos: 4,
    avance: '40%',
  };

  const displayCronograma = hasRealData ? cronogramaData.data.vistaGantt.map((fase) => {
    const hitosMapped = fase.hitos?.map((h) => {
      const start = new Date(h.fechaInicio);
      const end = new Date(h.fechaFin);
      const mesInicio = start.getUTCMonth();
      const duracion = (end.getUTCMonth() - start.getUTCMonth()) + 1;

      return {
        id: `h${h.hitoId}`,
        nombre: h.nombre,
        mesInicio,
        duracionMeses: Math.max(0.5, duracion),
        avance: h.avance || 0,
        fechaInicio: h.fechaInicio,
        fechaFin: h.fechaFin,
      };
    }) || [];

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
      duracionMeses: Math.max(1, mesFinFase - mesInicioFase),
      avance: fase.avance || 0,
      hitos: hitosMapped,
    };
  }) : [
    {
      id: 'f1',
      nombre: 'F1: Análisis y Diseño del Sistema (Demo)',
      mesInicio: 0,
      duracionMeses: 4,
      avance: 100,
      hitos: [
        { id: 'h1', nombre: 'Levantamiento y Validación de Requisitos', mesInicio: 0, duracionMeses: 3, avance: 100, fechaInicio: '2026-01-01', fechaFin: '2026-03-31' },
        { id: 'h2', nombre: 'Diseño Técnico del Sistema', mesInicio: 2, duracionMeses: 2, avance: 50, fechaInicio: '2026-03-01', fechaFin: '2026-04-30' },
      ],
    },
    {
      id: 'f2',
      nombre: 'F2: Desarrollo e Implementación (Demo)',
      mesInicio: 3,
      duracionMeses: 5,
      avance: 0,
      hitos: [
        { id: 'h3', nombre: 'Desarrollo del Sistema y Pruebas Técnicas', mesInicio: 4, duracionMeses: 2, avance: 0, fechaInicio: '2026-05-01', fechaFin: '2026-06-30' },
        { id: 'h4', nombre: 'Implementación Completa y Capacitación', mesInicio: 6, duracionMeses: 2, avance: 0, fechaInicio: '2026-07-01', fechaFin: '2026-08-31' },
      ],
    },
  ];

  const meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];

  return (
    <div className="cronograma-container">
      <CronogramaHeader proyectoId={proyectoId} />

      {cronogramaError && (
        <div className="error-container">
          <AlertCircle size={20} />
          <span>Error al cargar los datos del cronograma. Mostrando vista previa.</span>
        </div>
      )}

      <div className="top-cards-grid">
        <DocumentUpload
          proyectoId={proyectoId}
          tipoDocumento="CRONOGRAMA"
          label="Cronograma del Proyecto"
          onUploadSuccess={handleCronogramaUpload}
        />
        <ProjectInfoCard displayResumen={displayResumen} />
      </div>

      <GanttChart displayCronograma={displayCronograma} meses={meses} />
    </div>
  );
};

export default CronogramaPage;
