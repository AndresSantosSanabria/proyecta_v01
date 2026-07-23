import { useEffect, useState } from 'react';
import { AlertCircle, CheckCircle2, FolderOpen, Loader2, RefreshCw, Save, TestTube } from 'lucide-react';
import securityService from '../../services/securityService';
import './StorageConfigPanel.css';

const STORAGE_KEY = 'storage_base_path';

const StorageConfigPanel = () => {
  const [path, setPath] = useState('');
  const [originalPath, setOriginalPath] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [testResult, setTestResult] = useState(null);

  useEffect(() => {
    loadParameter();
  }, []);

  const loadParameter = async () => {
    try {
      setLoading(true);
      setError('');
      setTestResult(null);
      const params = await securityService.listSystemParameters();
      const list = Array.isArray(params) ? params : [];
      const stored = list.find((p) => p.key === STORAGE_KEY);
      const value = stored?.value || '';
      setPath(value);
      setOriginalPath(value);
    } catch {
      setError('Error al cargar la configuracion de almacenamiento.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      setError('');
      setNotice('');
      await securityService.saveSystemParameter({
        key: STORAGE_KEY,
        value: path.trim(),
        descripcion: 'Ruta padre de almacenamiento de archivos. Si esta vacia se usa la ruta por defecto del servidor.',
      });
      setOriginalPath(path.trim());
      setNotice('Ruta de almacenamiento actualizada correctamente.');
    } catch {
      setError('Error al guardar la ruta de almacenamiento.');
    } finally {
      setSaving(false);
    }
  };

  const handleTest = async () => {
    if (!path.trim()) {
      setTestResult({ valid: false, error: 'Ingrese una ruta para validar.' });
      return;
    }
    try {
      setTesting(true);
      setTestResult(null);
      setError('');
      const result = await securityService.testStoragePath(path.trim());
      setTestResult(result);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Error al validar la ruta.';
      setTestResult({ valid: false, error: msg });
    } finally {
      setTesting(false);
    }
  };

  const hasChanges = path.trim() !== originalPath;

  if (loading) {
    return (
      <div className="scp-loading">
        <Loader2 size={20} className="animate-spin" />
        <span>Cargando configuracion...</span>
      </div>
    );
  }

  return (
    <article className="panel panel-main scp-panel">
      <div className="panel-topbar">
        <div>
          <p className="security-eyebrow">CONFIGURACION DEL SISTEMA</p>
          <h2>Ruta de Almacenamiento</h2>
        </div>
        <div className="scp-topbar-actions">
          <button type="button" className="btn-secondary" onClick={loadParameter} disabled={loading}>
            <RefreshCw size={14} />
            Recargar
          </button>
        </div>
      </div>

      <div className="scp-body">
        {error && (
          <div className="feedback-banner error">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {notice && (
          <div className="feedback-banner success">
            <CheckCircle2 size={16} />
            <span>{notice}</span>
          </div>
        )}

        <div className="scp-info-card">
          <FolderOpen size={20} />
          <div>
            <p className="scp-info-title">Directorio raiz de archivos</p>
            <p className="scp-info-desc">
              Ruta absoluta donde se guardan todos los archivos del sistema (documentos, cronogramas, evidencias, etc.).
              Si se deja vacia, se usa la carpeta <code>uploads/</code> junto al ejecutable del servidor.
            </p>
          </div>
        </div>

        <div className="scp-form">
          <div className="scp-field">
            <label htmlFor="storage-path" className="scp-label">Ruta Padre</label>
            <div className="scp-input-row">
              <input
                id="storage-path"
                type="text"
                className="scp-input"
                value={path}
                onChange={(e) => { setPath(e.target.value); setTestResult(null); }}
                placeholder="Ej: C:\Proyecta\archivos o /var/data/proyecta"
                spellCheck={false}
              />
              <button
                type="button"
                className="btn-secondary scp-test-btn"
                onClick={handleTest}
                disabled={testing || !path.trim()}
              >
                {testing ? <Loader2 size={14} className="animate-spin" /> : <TestTube size={14} />}
                probar
              </button>
            </div>
            <p className="scp-hint">
              Use una ruta absoluta. El sistema creara subcarpetas automaticamente si no existen.
            </p>
          </div>

          {testResult && (
            <div className={`scp-test-result ${testResult.valid ? 'valid' : 'invalid'}`}>
              {testResult.valid ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              <div>
                {testResult.valid ? (
                  <>
                    <span className="scp-test-title">Ruta valida</span>
                    <span className="scp-test-detail">
                      {testResult.existed ? 'Directorio existente' : 'Directorio creado automaticamente'}
                      {testResult.writable ? ' con permisos de escritura' : ' sin permisos de escritura'}
                    </span>
                  </>
                ) : (
                  <>
                    <span className="scp-test-title">Ruta invalida</span>
                    <span className="scp-test-detail">{testResult.error}</span>
                  </>
                )}
              </div>
            </div>
          )}

          <div className="scp-actions">
            <button
              type="button"
              className="btn-primary"
              onClick={handleSave}
              disabled={saving || !hasChanges}
            >
              {saving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
              Guardar
            </button>
          </div>
        </div>
      </div>
    </article>
  );
};

export default StorageConfigPanel;
