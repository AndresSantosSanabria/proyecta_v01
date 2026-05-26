import React, { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AlertTriangle, Plus, RefreshCw, Radar, ShieldAlert, Sparkles, Trash2, Table2 } from 'lucide-react';
import riskService from '../../services/riskService';
import { usePermission } from '../../hooks/usePermission';
import './RiesgosPage.css';

const DEFAULT_PROBABILIDADES = ['BAJA', 'MEDIA', 'ALTA'];
const DEFAULT_IMPACTOS = ['BAJO', 'MEDIO', 'ALTO'];
const ESTADOS = ['PENDIENTE', 'TRATADO'];
const DEFAULT_NIVELES = ['BAJO', 'MODERADO', 'ALTO', 'CRITICO'];

const emptyForm = {
  categoriaRiesgo: '',
  descripcion: '',
  causa: '',
  consecuencia: '',
  probabilidad: 'MEDIA',
  impacto: 'MEDIO',
  probabilidadResidual: '',
  impactoResidual: '',
  controlesExistentes: '',
  tipoControl: '',
  valoracionControl: '',
  tratamiento: '',
  accionesMitigacion: '',
  entidadResponsable: '',
  rolResponsable: '',
  fechaAccion: '',
  evidenciaIndicador: '',
  estado: 'PENDIENTE',
};

const toNumber = (value) => {
  if (value == null) return 0;
  const parsed = typeof value === 'string' ? parseFloat(value) : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const RiesgosPage = () => {
  const params = useParams();
  const proyectoId = (params.codigoProyecto || params.id || '').toUpperCase();
  const canEdit = usePermission('PROYECTO:EDITAR');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [riskList, setRiskList] = useState([]);
  const [matrix, setMatrix] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [riskResp, matrixResp] = await Promise.all([
        riskService.getRiskList(proyectoId),
        riskService.getRiskMatrix(),
      ]);

      const riskData = riskResp?.data?.riesgos ?? riskResp?.data ?? [];
      const matrixData = matrixResp?.data ?? [];

      setRiskList(Array.isArray(riskData) ? riskData : []);
      setMatrix(Array.isArray(matrixData) ? matrixData : []);
      setError(null);
    } catch (err) {
      console.error(err);
      setError('No fue posible cargar la matriz de riesgos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [proyectoId]);

  const matrixLookup = useMemo(() => {
    return matrix.reduce((acc, item) => {
      acc[`${item.probabilidad}|${item.impacto}`] = item;
      return acc;
    }, {});
  }, [matrix]);

  const matrixProbabilidades = useMemo(() => {
    const seen = new Set(matrix.map((item) => item.probabilidad).filter(Boolean));
    const ordered = DEFAULT_PROBABILIDADES.filter((value) => seen.has(value));
    return ordered.length > 0 ? ordered : DEFAULT_PROBABILIDADES;
  }, [matrix]);

  const matrixImpactos = useMemo(() => {
    const seen = new Set(matrix.map((item) => item.impacto).filter(Boolean));
    const ordered = DEFAULT_IMPACTOS.filter((value) => seen.has(value));
    return ordered.length > 0 ? ordered : DEFAULT_IMPACTOS;
  }, [matrix]);

  const matrixRows = useMemo(
    () =>
      matrixProbabilidades.map((prob) => ({
        prob,
        cells: matrixImpactos.map((imp) => matrixLookup[`${prob}|${imp}`] || null),
      })),
    [matrixImpactos, matrixLookup, matrixProbabilidades]
  );

  const stats = useMemo(() => {
    const counts = { BAJO: 0, MODERADO: 0, ALTO: 0, CRITICO: 0 };
    riskList.forEach((risk) => {
      const key = String(risk.nivel || '').toUpperCase();
      if (counts[key] !== undefined) counts[key] += 1;
    });
    return counts;
  }, [riskList]);

  const deriveInherentScore = (probability, impact) => {
    const p = String(probability || '').toUpperCase();
    const i = String(impact || '').toUpperCase();
    const pMap = { BAJA: 1, MEDIA: 2, ALTA: 3 };
    const iMap = { BAJO: 1, MEDIO: 2, ALTO: 3 };
    return (pMap[p] || 0) * (iMap[i] || 0);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canEdit) return;

    try {
      setSaving(true);
      const payload = {
        categoriaRiesgo: form.categoriaRiesgo,
        descripcion: form.descripcion,
        causa: form.causa,
        consecuencia: form.consecuencia,
        probabilidad: form.probabilidad,
        impacto: form.impacto,
        probabilidadResidual: form.probabilidadResidual || null,
        impactoResidual: form.impactoResidual || null,
        controlesExistentes: form.controlesExistentes,
        tipoControl: form.tipoControl,
        valoracionControl: form.valoracionControl,
        tratamiento: form.tratamiento,
        accionesMitigacion: form.accionesMitigacion,
        entidadResponsable: form.entidadResponsable,
        rolResponsable: form.rolResponsable,
        fechaAccion: form.fechaAccion || null,
        evidenciaIndicador: form.evidenciaIndicador,
        estado: form.estado,
      };

      if (editingId) {
        await riskService.updateRisk(proyectoId, editingId, payload);
      } else {
        await riskService.createRisk(proyectoId, payload);
      }

      setForm(emptyForm);
      setEditingId(null);
      await fetchData();
    } catch (err) {
      setError(err?.response?.data?.detail || 'No fue posible guardar el riesgo.');
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (risk) => {
    setEditingId(risk.id);
    setForm({
      categoriaRiesgo: risk.categoriaRiesgo || '',
      descripcion: risk.descripcion || '',
      causa: risk.causa || '',
      consecuencia: risk.consecuencia || '',
      probabilidad: risk.probabilidad || 'MEDIA',
      impacto: risk.impacto || 'MEDIO',
      probabilidadResidual: risk.probabilidadResidual || '',
      impactoResidual: risk.impactoResidual || '',
      controlesExistentes: risk.controlesExistentes || '',
      tipoControl: risk.tipoControl || '',
      valoracionControl: risk.valoracionControl || '',
      tratamiento: risk.tratamiento || '',
      accionesMitigacion: risk.accionesMitigacion || '',
      entidadResponsable: risk.entidadResponsable || '',
      rolResponsable: risk.rolResponsable || '',
      fechaAccion: risk.fechaAccion || '',
      evidenciaIndicador: risk.evidenciaIndicador || '',
      estado: risk.estado || 'PENDIENTE',
    });
  };

  const removeRisk = async (riskId) => {
    if (!canEdit) return;
    if (!window.confirm('¿Eliminar este riesgo?')) return;
    await riskService.deleteRisk(proyectoId, riskId);
    await fetchData();
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  if (loading) {
    return (
      <div className="riesgos-page">
        <div className="empty-box loading-box">Cargando riesgos...</div>
      </div>
    );
  }

  return (
    <div className="riesgos-page">
      <section className="hero-card">
        <div className="hero-copy">
          <span className="eyebrow">
            <Radar size={14} />
            Gobernanza de riesgos
          </span>
          <h1>Matriz de Riesgos</h1>
          <p>
            La vista reproduce el formato de control institucional: identificación, valoración inherente, controles,
            riesgo residual y responsables.
          </p>
        </div>
        <div className="hero-metrics">
          <article>
            <Sparkles size={16} />
            <span>Catálogo activo</span>
            <strong>{matrix.length} cruces</strong>
          </article>
          <article>
            <ShieldAlert size={16} />
            <span>Niveles disponibles</span>
            <strong>{DEFAULT_NIVELES.length}</strong>
          </article>
          <button className="btn-refresh" onClick={fetchData} type="button">
            <RefreshCw size={16} /> Refrescar
          </button>
        </div>
      </section>

      {error && (
        <div className="riesgos-alert">
          <AlertTriangle size={16} />
          {error}
        </div>
      )}

      <div className="risk-stats-grid">
        <div className="risk-stat bajo">
          <span>Bajo</span>
          <strong>{stats.BAJO}</strong>
        </div>
        <div className="risk-stat moderado">
          <span>Moderado</span>
          <strong>{stats.MODERADO}</strong>
        </div>
        <div className="risk-stat alto">
          <span>Alto</span>
          <strong>{stats.ALTO}</strong>
        </div>
        <div className="risk-stat critico">
          <span>Crítico</span>
          <strong>{stats.CRITICO}</strong>
        </div>
      </div>

      <div className="risk-layout wide-risk-layout">
        <section className="panel risks-table-panel">
          <div className="panel-title">
            <div>
              <h2>Registro de riesgos</h2>
              <span className="panel-subtitle">Formato técnico de seguimiento</span>
            </div>
            <span className="panel-chip">{riskList.length} filas</span>
          </div>

          <div className="wide-table-wrap">
            <table className="risk-table">
              <thead>
                <tr>
                  <th rowSpan={2}>#</th>
                  <th rowSpan={2}>Categoría del riesgo</th>
                  <th rowSpan={2}>Descripción del riesgo</th>
                  <th rowSpan={2}>Causa</th>
                  <th rowSpan={2}>Consecuencia</th>
                  <th colSpan={4}>Riesgo Inherente</th>
                  <th colSpan={3}>Valoración de Controles</th>
                  <th colSpan={3}>Riesgo Residual</th>
                  <th rowSpan={2}>Tratamiento</th>
                  <th rowSpan={2}>Acciones de Mitigación</th>
                  <th rowSpan={2}>Entidad Responsable</th>
                  <th rowSpan={2}>Rol Responsable</th>
                  <th rowSpan={2}>Fecha de la Acción</th>
                  <th rowSpan={2}>Evidencia / Indicador</th>
                  <th rowSpan={2}>Acciones</th>
                </tr>
                <tr>
                  <th>Prob.</th>
                  <th>Impacto</th>
                  <th>Calificación</th>
                  <th>Nivel</th>
                  <th>Controles</th>
                  <th>Tipo</th>
                  <th>Valoración</th>
                  <th>Prob.</th>
                  <th>Impacto</th>
                  <th>Nivel</th>
                </tr>
              </thead>
              <tbody>
                {riskList.length === 0 ? (
                  <tr>
                    <td colSpan={21}>
                      <div className="empty-box">No hay riesgos registrados para este proyecto.</div>
                    </td>
                  </tr>
                ) : (
                  riskList.map((risk, index) => {
                    const score = deriveInherentScore(risk.probabilidad, risk.impacto);
                    return (
                      <tr key={risk.id}>
                        <td>{index + 1}</td>
                        <td>{risk.categoriaRiesgo || '—'}</td>
                        <td className="cell-wide">{risk.descripcion}</td>
                        <td className="cell-wide">{risk.causa || '—'}</td>
                        <td className="cell-wide">{risk.consecuencia || '—'}</td>
                        <td>
                          <span className="code-pill">{risk.probabilidad || '—'}</span>
                        </td>
                        <td>
                          <span className="code-pill">{risk.impacto || '—'}</span>
                        </td>
                        <td>
                          <span className="code-pill">{score || '—'}</span>
                        </td>
                        <td>
                          <span className={`risk-badge ${String(risk.nivel || '').toLowerCase()}`}>
                            {risk.nivel || 'SIN NIVEL'}
                          </span>
                        </td>
                        <td className="cell-wide">{risk.controlesExistentes || '—'}</td>
                        <td>{risk.tipoControl || '—'}</td>
                        <td>{risk.valoracionControl || '—'}</td>
                        <td>
                          <span className="code-pill">{risk.probabilidadResidual || '—'}</span>
                        </td>
                        <td>
                          <span className="code-pill">{risk.impactoResidual || '—'}</span>
                        </td>
                        <td>
                          <span className={`risk-badge ${String(risk.nivelResidual || '').toLowerCase()}`}>
                            {risk.nivelResidual || '—'}
                          </span>
                        </td>
                        <td className="cell-wide">{risk.tratamiento || '—'}</td>
                        <td className="cell-wide">{risk.accionesMitigacion || '—'}</td>
                        <td>{risk.entidadResponsable || '—'}</td>
                        <td>{risk.rolResponsable || '—'}</td>
                        <td>{risk.fechaAccion || '—'}</td>
                        <td className="cell-wide">{risk.evidenciaIndicador || '—'}</td>
                        <td>
                          <div className="row-actions">
                            <button type="button" className="btn-secondary compact" onClick={() => startEdit(risk)} disabled={!canEdit}>
                              Editar
                            </button>
                            <button type="button" className="btn-danger compact" onClick={() => removeRisk(risk.id)} disabled={!canEdit}>
                              <Trash2 size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="panel-title">
            <div>
              <h2>{editingId ? 'Editar riesgo' : 'Nuevo riesgo'}</h2>
              <span className="panel-subtitle">CRUD real sobre el proyecto</span>
            </div>
            <span className="panel-chip">{canEdit ? 'Modo edición' : 'Solo lectura'}</span>
          </div>

          <form className="risk-form" onSubmit={handleSubmit}>
            <label>
              Categoría del riesgo
              <input
                value={form.categoriaRiesgo}
                onChange={(e) => setForm((prev) => ({ ...prev, categoriaRiesgo: e.target.value }))}
                placeholder="Estratégico, operativo, tecnológico..."
              />
            </label>
            <label>
              Descripción del riesgo
              <textarea
                value={form.descripcion}
                onChange={(e) => setForm((prev) => ({ ...prev, descripcion: e.target.value }))}
                required
                rows={3}
              />
            </label>
            <label>
              Causa
              <textarea
                value={form.causa}
                onChange={(e) => setForm((prev) => ({ ...prev, causa: e.target.value }))}
                rows={3}
              />
            </label>
            <label>
              Consecuencia
              <textarea
                value={form.consecuencia}
                onChange={(e) => setForm((prev) => ({ ...prev, consecuencia: e.target.value }))}
                rows={3}
              />
            </label>

            <div className="form-grid">
              <label>
                Probabilidad inherente
                <select
                  value={form.probabilidad}
                  onChange={(e) => setForm((prev) => ({ ...prev, probabilidad: e.target.value }))}
                >
                  {matrixProbabilidades.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Impacto inherente
                <select value={form.impacto} onChange={(e) => setForm((prev) => ({ ...prev, impacto: e.target.value }))}>
                  {matrixImpactos.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Probabilidad residual
                <select
                  value={form.probabilidadResidual}
                  onChange={(e) => setForm((prev) => ({ ...prev, probabilidadResidual: e.target.value }))}
                >
                  <option value="">Sin definir</option>
                  {matrixProbabilidades.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Impacto residual
                <select value={form.impactoResidual} onChange={(e) => setForm((prev) => ({ ...prev, impactoResidual: e.target.value }))}>
                  <option value="">Sin definir</option>
                  {matrixImpactos.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label>
              Controles existentes
              <textarea
                value={form.controlesExistentes}
                onChange={(e) => setForm((prev) => ({ ...prev, controlesExistentes: e.target.value }))}
                rows={3}
              />
            </label>

            <div className="form-grid">
              <label>
                Tipo de control
                <input
                  value={form.tipoControl}
                  onChange={(e) => setForm((prev) => ({ ...prev, tipoControl: e.target.value }))}
                  placeholder="Preventivo, detectivo, correctivo..."
                />
              </label>
              <label>
                Valoración del control
                <input
                  value={form.valoracionControl}
                  onChange={(e) => setForm((prev) => ({ ...prev, valoracionControl: e.target.value }))}
                  placeholder="Alta, media, baja"
                />
              </label>
            </div>

            <label>
              Tratamiento
              <textarea
                value={form.tratamiento}
                onChange={(e) => setForm((prev) => ({ ...prev, tratamiento: e.target.value }))}
                rows={3}
              />
            </label>

            <label>
              Acciones de mitigación
              <textarea
                value={form.accionesMitigacion}
                onChange={(e) => setForm((prev) => ({ ...prev, accionesMitigacion: e.target.value }))}
                rows={3}
              />
            </label>

            <div className="form-grid">
              <label>
                Entidad responsable
                <input
                  value={form.entidadResponsable}
                  onChange={(e) => setForm((prev) => ({ ...prev, entidadResponsable: e.target.value }))}
                  placeholder="Secretaría TIC, despacho..."
                />
              </label>
              <label>
                Rol responsable
                <input
                  value={form.rolResponsable}
                  onChange={(e) => setForm((prev) => ({ ...prev, rolResponsable: e.target.value }))}
                  placeholder="Gerente del proyecto, líder..."
                />
              </label>
              <label>
                Fecha de la acción
                <input
                  type="date"
                  value={form.fechaAccion}
                  onChange={(e) => setForm((prev) => ({ ...prev, fechaAccion: e.target.value }))}
                />
              </label>
              <label>
                Estado
                <select value={form.estado} onChange={(e) => setForm((prev) => ({ ...prev, estado: e.target.value }))}>
                  {ESTADOS.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label>
              Evidencia / Indicador
              <textarea
                value={form.evidenciaIndicador}
                onChange={(e) => setForm((prev) => ({ ...prev, evidenciaIndicador: e.target.value }))}
                rows={3}
              />
            </label>

            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={resetForm}>
                Cancelar
              </button>
              <button className="btn-primary" disabled={!canEdit || saving} type="submit">
                <Plus size={16} /> {editingId ? 'Guardar cambios' : 'Crear riesgo'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  );
};

export default RiesgosPage;
