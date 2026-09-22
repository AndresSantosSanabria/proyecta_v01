import { useEffect, useState } from 'react';
import { AlertCircle, CheckCircle2, Clock, Loader2, Save, Settings, Bell } from 'lucide-react';
import advanceReportService from '../../services/advanceReportService';
import './AdvanceReportConfigPanel.css';

const DEFAULTS = {
  due_date: '',
  pre_due_window_days: '15',
  pre_due_interval_days: '3',
  post_due_interval_days: '7',
  specific_override_dates: '',
  allowed_extensions: 'pdf,pptx',
  max_size_mb: '20',
  login_modal_delay_ms: '1200',
};

const AdvanceReportConfigPanel = () => {
  const [settings, setSettings] = useState({ ...DEFAULTS });
  const [original, setOriginal] = useState({ ...DEFAULTS });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await advanceReportService.getSettings();
      const merged = { ...DEFAULTS, ...(data || {}) };
      setSettings(merged);
      setOriginal(merged);
    } catch {
      setError('Error al cargar la configuracion del informe de avance.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (key, value) => {
    setSettings((prev) => ({ ...prev, [key]: value }));
    setNotice('');
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      setError('');
      setNotice('');
      await advanceReportService.updateSettings(settings);
      setOriginal({ ...settings });
      setNotice('Configuracion guardada correctamente.');
    } catch {
      setError('Error al guardar la configuracion.');
    } finally {
      setSaving(false);
    }
  };

  const hasChanges = JSON.stringify(settings) !== JSON.stringify(original);

  if (loading) {
    return (
      <div className="arp-loading">
        <Loader2 size={24} className="arp-spin" />
        <span>Cargando configuracion...</span>
      </div>
    );
  }

  return (
    <div className="arp-panel">
      <div className="arp-header">
        <div className="arp-header-icon">
          <Settings size={20} />
        </div>
        <div>
          <h3>Configuracion del Informe de Avance</h3>
          <p>Parametros de notificacion, ventanas de tiempo y modal de login.</p>
        </div>
      </div>

      {error && (
        <div className="arp-alert arp-alert--error">
          <AlertCircle size={15} />
          <span>{error}</span>
        </div>
      )}

      {notice && (
        <div className="arp-alert arp-alert--success">
          <CheckCircle2 size={15} />
          <span>{notice}</span>
        </div>
      )}

      <div className="arp-section">
        <h4><Clock size={15} /> Fechas y Ventanas</h4>
        <div className="arp-grid">
          <div className="arp-field">
            <label>Fecha limite del informe</label>
            <input
              type="date"
              value={settings.due_date}
              onChange={(e) => handleChange('due_date', e.target.value)}
            />
            <span className="arp-hint">Fecha tope para cargar el informe de avance.</span>
          </div>
          <div className="arp-field">
            <label>Ventana previa (dias)</label>
            <input
              type="number"
              min="1"
              max="60"
              value={settings.pre_due_window_days}
              onChange={(e) => handleChange('pre_due_window_days', e.target.value)}
            />
            <span className="arp-hint">Dias antes de la fecha limite para iniciar notificaciones.</span>
          </div>
          <div className="arp-field">
            <label>Intervalo previo (dias)</label>
            <input
              type="number"
              min="1"
              max="30"
              value={settings.pre_due_interval_days}
              onChange={(e) => handleChange('pre_due_interval_days', e.target.value)}
            />
            <span className="arp-hint">Cada cuantos dias se notifica antes de vencer.</span>
          </div>
          <div className="arp-field">
            <label>Intervalo post-vencimiento (dias)</label>
            <input
              type="number"
              min="1"
              max="30"
              value={settings.post_due_interval_days}
              onChange={(e) => handleChange('post_due_interval_days', e.target.value)}
            />
            <span className="arp-hint">Cada cuantos dias se notifica despues de vencido.</span>
          </div>
        </div>
      </div>

      <div className="arp-section">
        <h4><Bell size={15} /> Fechas Especificas (Override)</h4>
        <div className="arp-field arp-field--full">
          <label>Fechas de disparo manual</label>
          <input
            type="text"
            value={settings.specific_override_dates}
            onChange={(e) => handleChange('specific_override_dates', e.target.value)}
            placeholder="2026-10-01, 2026-10-15"
          />
          <span className="arp-hint">Fechas exactas en las que se envia notificacion sin importar la ventana. Separar con coma.</span>
        </div>
      </div>

      <div className="arp-section">
        <h4><Settings size={15} /> Archivos y Modal</h4>
        <div className="arp-grid">
          <div className="arp-field">
            <label>Extensiones permitidas</label>
            <input
              type="text"
              value={settings.allowed_extensions}
              onChange={(e) => handleChange('allowed_extensions', e.target.value)}
              placeholder="pdf,pptx"
            />
            <span className="arp-hint">Separadas por coma.</span>
          </div>
          <div className="arp-field">
            <label>Tamano maximo (MB)</label>
            <input
              type="number"
              min="1"
              max="100"
              value={settings.max_size_mb}
              onChange={(e) => handleChange('max_size_mb', e.target.value)}
            />
            <span className="arp-hint">Limite en megabytes por archivo.</span>
          </div>
          <div className="arp-field">
            <label>Delay del modal de login (ms)</label>
            <input
              type="number"
              min="0"
              max="10000"
              step="100"
              value={settings.login_modal_delay_ms}
              onChange={(e) => handleChange('login_modal_delay_ms', e.target.value)}
            />
            <span className="arp-hint">Milisegundos de espera antes de mostrar el modal tras login.</span>
          </div>
        </div>
      </div>

      <div className="arp-footer">
        <button
          className="arp-btn arp-btn--save"
          onClick={handleSave}
          disabled={saving || !hasChanges}
        >
          {saving ? <Loader2 size={14} className="arp-spin" /> : <Save size={14} />}
          Guardar cambios
        </button>
      </div>
    </div>
  );
};

export default AdvanceReportConfigPanel;
