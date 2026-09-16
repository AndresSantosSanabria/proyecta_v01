const CronogramaHeader = ({ proyectoId }) => {
  return (
    <header className="cronograma-header">
      <h1>Diagrama de Gantt</h1>
      <p>{proyectoId} - Fortalecimiento de Talento TI en Cundinamarca</p>
    </header>
  );
};

export default CronogramaHeader;
