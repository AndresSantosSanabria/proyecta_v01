import React, { useState, useEffect } from 'react';
import KPICard from '../components/common/KPICard';
import dashboardService from '../services/dashboardService';
import './DashboardPage.css';

const DashboardPage = () => {
  const [kpis, setKpis] = useState(null);
  const [loading, setLoading] = useState(true);
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
      } catch (err) {
        setError('No se pudo conectar con el servidor');
      } finally {
        setLoading(false);
      }
    };

    fetchKPIs();
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

  if (!kpis) return null;

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
    </div>
  );
};

export default DashboardPage;
