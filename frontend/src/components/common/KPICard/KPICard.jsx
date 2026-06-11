import './KPICard.css';

const KPICard = ({ title, value, detail, color = 'success', progress = 100 }) => {
  return (
    <div className="kpi-card">
      <div className="kpi-content">
        <span className="kpi-title">{title}</span>
        <h2 className={`kpi-value ${color}`}>{value}</h2>
        <span className="kpi-detail">{detail}</span>
      </div>
      <div className="kpi-progress-container">
        <div 
          className={`kpi-progress-bar ${color}`} 
          style={{ width: `${progress}%` }}
        ></div>
      </div>
    </div>
  );
};

export default KPICard;
