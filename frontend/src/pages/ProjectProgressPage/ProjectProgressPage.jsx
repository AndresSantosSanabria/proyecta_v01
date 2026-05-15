import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Lock, Info } from 'lucide-react';
import projectService from '../../services/projectService';
import ProgressHeader from '../../components/features/progress/ProgressHeader';
import ProgressKPIs from '../../components/features/progress/ProgressKPIs';
import ProgressTreeTable from '../../components/features/progress/ProgressTreeTable';
import './ProjectProgressPage.css';

const ProjectProgressPage = () => {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
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
    codigo: id,
    nombre: 'Cargando proyecto...',
    avanceTotal: 0,
    entregablesConformidad: 0,
    entregablesTotal: 0,
    atrasados: 0,
    proximosVencer: 0,
    fases: []
  });

  useEffect(() => {
    const fetchProgress = async () => {
      try {
        setLoading(true);
        const response = await projectService.getProgress(id);
        const apiData = response.data || response;
        
        setProgressData(prevData => ({
          ...prevData,
          nombre: apiData.nombre || apiData.nombreProyecto || prevData.nombre,
          avanceTotal: apiData.avanceTotal || apiData.avance || 0,
          entregablesConformidad: apiData.entregablesConformidad || apiData.entregablesConformes || 0,
          entregablesTotal: apiData.entregablesTotal || 0,
          atrasados: apiData.atrasados || apiData.entregablesAtrasados || 0,
          proximosVencer: apiData.proximosVencer || 0,
          fases: apiData.fases || getMockFases()
        }));
      } catch (err) {
        console.error('Error fetching project progress:', err);
        setError('No se pudo cargar la información del proyecto.');
        setProgressData(prev => ({
          ...prev,
          nombre: 'Fortalecimiento de Talento TI en Cundinamarca (Modo Demo)',
          avanceTotal: 40,
          entregablesConformidad: 2,
          entregablesTotal: 8,
          atrasados: 6,
          proximosVencer: 0,
          fases: getMockFases()
        }));
      } finally {
        setLoading(false);
      }
    };

    fetchProgress();
  }, [id]);

  const getMockFases = () => [
    {
        id: 'F1',
        nombre: 'Análisis y Diseño del Sistema',
        ponderacion: 50,
        avance: 80,
        estado: 'calculated',
        hitos: [
          {
            id: 'H1',
            nombre: 'Levantamiento y Validación de Requisitos',
            ponderacion: 80,
            avance: 100,
            estado: 'calculated',
            entregables: [
              {
                id: 'E1',
                nombre: 'Documento de Requisitos Funcional',
                ponderacion: 10,
                fechaLimite: '2025-02-15',
                avance: 100,
                estado: 'A conformidad',
                archivo: 'requisitos_func.pdf',
                fechaEntrega: '2025-02-14'
              },
              {
                id: 'E2',
                nombre: 'Acta de Validación de Requisitos',
                ponderacion: 90,
                fechaLimite: '2025-02-16',
                avance: 100,
                estado: 'A conformidad',
                archivo: 'acta_validacion.pdf',
                fechaEntrega: '2025-02-16'
              }
            ]
          },
          {
            id: 'H2',
            nombre: 'Diseño Técnico del Sistema',
            ponderacion: 20,
            avance: 0,
            estado: 'calculated',
            entregables: [
              {
                id: 'E3',
                nombre: 'Prototipo de Interfaces de Usuario',
                ponderacion: 70,
                fechaLimite: '2025-04-14',
                avance: 0,
                estado: 'Atrasado',
                atraso: '+359d'
              },
              {
                id: 'E4',
                nombre: 'Diseño de Arquitectura del Sistema',
                ponderacion: 30,
                fechaLimite: '2025-04-15',
                avance: 0,
                estado: 'Atrasado',
                atraso: '+358d'
              }
            ]
          }
        ]
      },
      {
        id: 'F2',
        nombre: 'Desarrollo e Implementación',
        ponderacion: 50,
        avance: 0,
        estado: 'calculated',
        hitos: [
          {
            id: 'H3',
            nombre: 'Desarrollo del Sistema y Pruebas Técnicas',
            ponderacion: 50,
            avance: 0,
            estado: 'calculated',
            entregables: [
              {
                id: 'E5',
                nombre: 'Módulos del Sistema Desarrollados',
                ponderacion: 50,
                fechaLimite: '2025-07-15',
                avance: 0,
                estado: 'Atrasado',
                atraso: '+267d'
              }
            ]
          }
        ]
      }
    ];

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

      <ProgressTreeTable 
        progressData={progressData} 
        isExpanded={isExpanded} 
        toggleNode={toggleNode} 
      />
    </div>
  );
};

export default ProjectProgressPage;

