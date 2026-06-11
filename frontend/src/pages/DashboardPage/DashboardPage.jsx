import { useState, useEffect } from 'react';
import KPICard from '../../components/common/KPICard';
import ProjectTable from '../../components/features/projects/ProjectTable';
import dashboardService from '../../services/dashboardService';
import './DashboardPage.css';

const DashboardPage = () => {
  const [kpis, setKpis] = useState(null);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchKPIs = async () => {
      try {
        setLoading(true);
        const response = await dashboardService.getKPIs();
        if (response.success) {
          setKpis(response.data);
        } else {
          setError(response.message || 'Error al obtener las métricas');
        }
      } catch {
        setError('No se pudo conectar con el servidor');
      } finally {
        setLoading(false);
      }
    };

    const fetchProjects = async () => {
      try {
        setLoadingProjects(true);
        const response = await dashboardService.getProjects();
        if (response.success) {
          setProjects(response.data);
        }
      } catch (err) {
        console.error('Error fetching projects:', err);
        // Fallback for demo if endpoint is not ready
        setProjects([
          {
            id: 1,
            codigo: 'IS-PROY-CUN-001',
            nombreProyecto: 'Modernización del Data Center Principal',
            nombreDependencia: 'Infraestructura',
            avance: 20,
            estado: 'CON_RETRASOS',
            entregablesAtrasados: 1
          },
          {
            id: 2,
            codigo: 'IS-PROY-CUN-002',
            nombreProyecto: 'Sistema de PQRS Ciudadano',
            nombreDependencia: 'Atención al Ciudadano',
            avance: 15,
            estado: 'CON_RETRASOS',
            entregablesAtrasados: 2
          },
          {
            id: 3,
            codigo: 'IS-PROY-CUN-004',
            nombreProyecto: 'Portal Web Gobernación 2.0',
            nombreDependencia: 'Prensa y Comunicaciones',
            avance: 72,
            estado: 'ACTIVO',
            entregablesAtrasados: 0
          }
        ]);
      } finally {
        setLoadingProjects(false);
      }
    };

    fetchKPIs();
    fetchProjects();
  }, []);

  if (loading) {
    return (
      <div className="dashboard-container">
        <div className="loading-state">Cargando métricas...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-container">
        <div className="error-state">{error}</div>
      </div>
    );
  }

  if (!kpis) {
    return (
      <div className="dashboard-container">
        <div className="error-state">No se pudieron cargar las métricas del dashboard.</div>
      </div>
    );
  }

  const kpiData = [
    {
      title: 'Total Proyectos',
      value: kpis.total_proyectos.toString(),
      detail: `${kpis.activos} activos · ${kpis.cerrados} cerrados`,
      color: 'success',
      progress: 100
    },
    {
      title: 'Avance Promedio',
      value: `${kpis.avance_promedio}%`,
      detail: `Tendencia: ${kpis.avance_tendencia}`,
      color: kpis.avance_promedio < 50 ? 'danger' : 'success',
      progress: kpis.avance_promedio
    },
    {
      title: 'Entregables Atrasados',
      value: kpis.entregables_atrasados.toString(),
      detail: 'En todos los proyectos',
      color: kpis.entregables_atrasados > 0 ? 'danger' : 'success',
      progress: 100
    },
    {
      title: 'Próximos a Vencer',
      value: kpis.proximos_a_vencer.toString(),
      detail: `Vencen en menos de ${kpis.dias_ventana_vencimiento} días`,
      color: kpis.proximos_a_vencer > 5 ? 'warning' : 'success',
      progress: 0
    }
  ];

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Vista Dashboard</h1>
      </header>

      <div className="kpi-grid">
        {kpiData.map((kpi, index) => (
          <KPICard 
            key={index}
            title={kpi.title}
            value={kpi.value}
            detail={kpi.detail}
            color={kpi.color}
            progress={kpi.progress}
          />
        ))}
      </div>

      <ProjectTable 
        projects={projects} 
        loading={loadingProjects} 
      />
    </div>
  );
};

export default DashboardPage;
