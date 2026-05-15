import React, { useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  AlertCircle, 
  Loader2,
  CheckCircle2
} from 'lucide-react';
import cronogramaService from '../../services/cronogramaService';
import CronogramaHeader from '../../components/features/cronograma/CronogramaHeader';
import UploadCronogramaCard from '../../components/features/cronograma/UploadCronogramaCard';
import ProjectInfoCard from '../../components/features/cronograma/ProjectInfoCard';
import GanttChart from '../../components/features/cronograma/GanttChart';
import './CronogramaPage.css';

const CronogramaPage = () => {
  const { id: proyectoId } = useParams();
  const queryClient = useQueryClient();
  const fileInputRef = useRef(null);
  const [uploadMessage, setUploadMessage] = useState({ text: '', type: '' });

  // Queries
  const { 
    data: cronogramaData, 
    isLoading: isLoadingCronograma, 
    error: cronogramaError 
  } = useQuery({
    queryKey: ['cronograma', proyectoId],
    queryFn: () => cronogramaService.getCronograma(proyectoId),
  });

  const { 
    data: resumenData, 
    isLoading: isLoadingResumen 
  } = useQuery({
    queryKey: ['resumen', proyectoId],
    queryFn: () => cronogramaService.getResumenProyecto(proyectoId),
  });

  // Mutations
  const uploadMutation = useMutation({
    mutationFn: (file) => cronogramaService.uploadCronograma(proyectoId, file),
    onSuccess: (response) => {
      setUploadMessage({ 
        text: response.message || 'Cronograma subido con éxito', 
        type: 'success' 
      });
      queryClient.invalidateQueries({ queryKey: ['cronograma', proyectoId] });
      setTimeout(() => setUploadMessage({ text: '', type: '' }), 5000);
    },
    onError: (error) => {
      setUploadMessage({ 
        text: 'Error al subir el archivo. Inténtalo de nuevo.', 
        type: 'error' 
      });
    }
  });

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      uploadMutation.mutate(file);
    } else if (file) {
      setUploadMessage({ text: 'Por favor, selecciona un archivo PDF.', type: 'error' });
    }
  };

  const handleDownload = async () => {
    try {
      const blob = await cronogramaService.downloadCronograma(proyectoId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `cronograma_${proyectoId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (error) {
      setUploadMessage({ text: 'Error al descargar el archivo.', type: 'error' });
    }
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

  // Map real data from backend or use fallback for visual demonstration
  const displayResumen = resumenData?.data ? {
    fechaInicio: resumenData.data.fechaInicio || 'Pendiente', 
    director: resumenData.data.director || 'No asignado',
    fases: resumenData.data.totalFases || 0,
    totalHitos: resumenData.data.totalHitos || 0,
    avance: `${(resumenData.data.avanceTotal || 0).toFixed(0)}%`
  } : {
    fechaInicio: '2025-01-15',
    director: 'Luis Guillermo Montenegro',
    fases: 2,
    totalHitos: 4,
    avance: '40%'
  };

  // Process real Gantt data from API or use fallback
  const displayCronograma = cronogramaData?.data?.vistaGantt?.map(fase => {
    const hitosMapped = fase.hitos?.map(h => {
      const start = new Date(h.fechaInicio);
      const end = new Date(h.fechaFin);
      const mesInicio = start.getUTCMonth();
      const duracion = (end.getUTCMonth() - start.getUTCMonth()) + 1;
      
      return {
        id: `h${h.hitoId}`,
        nombre: h.nombre,
        mesInicio: mesInicio,
        duracionMeses: Math.max(0.5, duracion),
        avance: h.avance || 0,
        fechaInicio: h.fechaInicio,
        fechaFin: h.fechaFin
      };
    }) || [];

    const mesInicioFase = hitosMapped.length > 0 
      ? Math.min(...hitosMapped.map(h => h.mesInicio)) 
      : 0;
    
    const mesFinFase = hitosMapped.length > 0 
      ? Math.max(...hitosMapped.map(h => h.mesInicio + h.duracionMeses)) 
      : 1;

    return {
      id: `f${fase.faseId}`,
      nombre: fase.nombre,
      mesInicio: mesInicioFase,
      duracionMeses: Math.max(1, mesFinFase - mesInicioFase),
      avance: fase.avance || 0,
      hitos: hitosMapped
    };
  }) || [
    {
      id: 'f1',
      nombre: 'F1: Análisis y Diseño del Sistema (Demo)',
      mesInicio: 0,
      duracionMeses: 4,
      avance: 100,
      hitos: [
        { id: 'h1', nombre: 'Levantamiento y Validación d', mesInicio: 0, duracionMeses: 3, avance: 100, fechaInicio: '2026-01-01', fechaFin: '2026-03-31' },
        { id: 'h2', nombre: 'Diseño Técnico del Sistema', mesInicio: 2, duracionMeses: 2, avance: 50, fechaInicio: '2026-03-01', fechaFin: '2026-04-30' }
      ]
    },
    {
      id: 'f2',
      nombre: 'F2: Desarrollo e Implementación (Demo)',
      mesInicio: 3,
      duracionMeses: 5,
      avance: 0,
      hitos: [
        { id: 'h3', nombre: 'Desarrollo del Sistema y Pru', mesInicio: 4, duracionMeses: 2, avance: 0, fechaInicio: '2026-05-01', fechaFin: '2026-06-30' },
        { id: 'h4', nombre: 'Implementación Completa y Ca', mesInicio: 6, duracionMeses: 2, avance: 0, fechaInicio: '2026-07-01', fechaFin: '2026-08-31' }
      ]
    }
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

      {uploadMessage.text && (
        <div className={`error-container ${uploadMessage.type === 'success' ? 'success-msg' : ''}`} 
             style={{ backgroundColor: uploadMessage.type === 'success' ? '#e8f5e9' : '#ffebee', 
                      color: uploadMessage.type === 'success' ? '#2e7d32' : '#c62828' }}>
          {uploadMessage.type === 'success' ? <CheckCircle2 size={20} /> : <AlertCircle size={20} />}
          <span>{uploadMessage.text}</span>
        </div>
      )}

      <div className="top-cards-grid">
        <UploadCronogramaCard 
          proyectoId={proyectoId}
          uploadMutation={uploadMutation}
          handleUploadClick={handleUploadClick}
          handleFileChange={handleFileChange}
          handleDownload={handleDownload}
          fileInputRef={fileInputRef}
        />
        <ProjectInfoCard displayResumen={displayResumen} />
      </div>

      <GanttChart displayCronograma={displayCronograma} meses={meses} />
    </div>
  );
};

export default CronogramaPage;

