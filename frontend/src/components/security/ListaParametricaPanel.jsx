import { useEffect, useState } from 'react';
import {
  Pencil, Save, ArrowLeft, LoaderCircle,
} from 'lucide-react';
import configCatalogService from '../../services/configCatalogService';
import './ListaParametricaPanel.css';

const ListaParametricaPanel = () => {
  const [listas, setListas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingKey, setEditingKey] = useState(null);
  const [form, setForm] = useState({ nombreCampo: '', descripcion: '', valores: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  useEffect(() => { loadAll(); }, []);

  const getBackendError = (err) => {
    const data = err.response?.data;
    if (data?.detail) return data.detail;
    if (data?.message) return data.message;
    if (typeof data === 'string') return data;
    return 'Error al procesar la solicitud.';
  };

  const loadAll = async () => {
    try {
      setLoading(true);
      setError(null);
      const listasConocidas = ['DEPENDENCIA', 'ROL_EQUIPO', 'VIGENCIA_PETI', 'ESTRATEGIA_PETI', 'FURAG_PREGUNTAS', 'CARGO_ASIGNACION'];
      const results = [];
      for (const clave of listasConocidas) {
        try {
          const items = await configCatalogService.listarParametrica(clave, false);
          const arr = Array.isArray(items) ? items : [];
          const first = arr[0] || {};
          results.push({
            clave,
            nombreCampo: first.listaNombreCampo || clave,
            descripcion: first.listaDescripcion || '',
            tipo: first.listaTipo || 'Lista',
            valores: arr.filter((i) => i.activo).map((i) => i.itemNombre).join('\n'),
            count: arr.length,
          });
        } catch {
          results.push({ clave, nombreCampo: clave, descripcion: '', tipo: 'Lista', valores: '', count: 0 });
        }
      }
      setListas(results);
    } catch (err) {
      setError(getBackendError(err));
    } finally {
      setLoading(false);
    }
  };

  const openEdit = (lista) => {
    setEditingKey(lista.clave);
    setForm({
      nombreCampo: lista.nombreCampo || lista.clave,
      descripcion: lista.descripcion || '',
      valores: lista.valores || '',
    });
    setError(null);
  };

  const handleSave = async () => {
    if (!form.nombreCampo.trim()) {
      setError('El nombre del campo es obligatorio.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      const valores = form.valores
        .split('\n')
        .map((v) => v.trim())
        .filter((v) => v.length > 0);
      await configCatalogService.guardarValoresLista(editingKey, {
        nombreCampo: form.nombreCampo.trim(),
        descripcion: form.descripcion.trim(),
        valores,
      });
      setNotice('Valores guardados correctamente.');
      setEditingKey(null);
      await loadAll();
      setTimeout(() => setNotice(null), 3000);
    } catch (err) {
      setError(getBackendError(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="lpp-container">
        <div className="lpp-loading-center">
          <LoaderCircle size={20} className="spin" /> Cargando configuracion...
        </div>
      </div>
    );
  }

  if (editingKey) {
    const current = listas.find((l) => l.clave === editingKey);
    return (
      <div className="lpp-container">
        <div className="lpp-header">
          <div className="lpp-header-text">
            <button type="button" className="lpp-back-btn" onClick={() => setEditingKey(null)}>
              <ArrowLeft size={14} /> Volver
            </button>
            <p className="security-eyebrow">EDITAR CAMPO</p>
            <h2>{current?.nombreCampo || editingKey}</h2>
          </div>
        </div>

        {notice && <div className="feedback-banner success">{notice}</div>}
        {error && <div className="feedback-banner error">{error}</div>}

        <div className="lpp-edit-form">
          <div className="lpp-edit-field">
            <label>Formato</label>
            <select className="lpp-input" disabled value="Lista">
              <option value="Lista">Lista</option>
            </select>
          </div>

          <div className="lpp-edit-field">
            <label>Nombre *</label>
            <input
              type="text"
              className="lpp-input"
              value={form.nombreCampo}
              onChange={(e) => setForm((p) => ({ ...p, nombreCampo: e.target.value }))}
              placeholder="Nombre del campo"
            />
          </div>

          <div className="lpp-edit-field">
            <label>Descripcion</label>
            <textarea
              className="lpp-input lpp-textarea-small"
              value={form.descripcion}
              onChange={(e) => setForm((p) => ({ ...p, descripcion: e.target.value }))}
              placeholder="Descripcion opcional del campo"
              rows={2}
            />
          </div>

          <div className="lpp-edit-field">
            <label>Valores posibles *</label>
            <textarea
              className="lpp-input lpp-textarea"
              value={form.valores}
              onChange={(e) => setForm((p) => ({ ...p, valores: e.target.value }))}
              placeholder={'Ingrese un valor por linea.\nEjemplo:\nInfraestructura\nFinanzas\nPlaneacion'}
              rows={12}
            />
            <span className="lpp-hint">Un valor por linea. Los valores vacios se ignoran.</span>
          </div>

          <div className="lpp-edit-actions">
            <button type="button" className="btn-secondary" onClick={() => setEditingKey(null)}>
              Cancelar
            </button>
            <button type="button" className="btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? <><LoaderCircle size={14} className="spin" /> Guardando...</> : <><Save size={14} /> Guardar</>}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="lpp-container">
      <div className="lpp-header">
        <div className="lpp-header-text">
          <p className="security-eyebrow">CONFIGURACION DE LISTAS</p>
          <h2>Editar campos de las listas</h2>
          <p className="lpp-subtitle">Administre los valores que aparecen en los desplegables del sistema.</p>
        </div>
      </div>

      {notice && <div className="feedback-banner success">{notice}</div>}
      {error && <div className="feedback-banner error">{error}</div>}

      <div className="lpp-table-shell">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Descripcion</th>
              <th style={{ width: 100 }}>Tipo</th>
              <th style={{ width: 140 }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {listas.map((lista) => (
              <tr key={lista.clave}>
                <td>{lista.nombreCampo}</td>
                <td className="lpp-desc-cell">{lista.descripcion || <span className="lpp-no-desc">Sin descripcion</span>}</td>
                <td>{lista.tipo}</td>
                <td className="actions-cell">
                  <button
                    type="button"
                    className="btn-secondary btn-sm"
                    onClick={() => openEdit(lista)}
                  >
                    <Pencil size={13} /> Editar
                  </button>
                </td>
              </tr>
            ))}
            {listas.length === 0 && (
              <tr>
                <td colSpan={4} className="lpp-empty-cell">No hay campos configurados.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ListaParametricaPanel;
