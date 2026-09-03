import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  AlertTriangle,
  Download,
  Eye,
  FileUp,
  Info,
  Plus,
  RefreshCw,
  Radar,
  ShieldAlert,
  Sparkles,
  Trash2,
  X,
  Save,
  Pencil,
} from 'lucide-react';
import riskService from '../../services/riskService';
import { usePermission } from '../../hooks/usePermission';
import SpellCheckerTextarea from '../../components/common/SpellCheckerTextarea';
import SpellCheckerInput from '../../components/common/SpellCheckerInput';
import { AutocompleteSelect } from '../../components/common/AutocompleteSelect';
import './RiesgosPage.css';

const DEFAULT_PROBABILIDADES = ['BAJA', 'MEDIA', 'ALTA'];
const DEFAULT_IMPACTOS = ['BAJO', 'MEDIO', 'ALTO'];
const ESTADOS = ['PENDIENTE', 'TRATADO'];
const DEFAULT_NIVELES = ['BAJO', 'MODERADO', 'ALTO', 'EXTREMO'];
const RISK_LEVEL_LABELS = {
  BAJO: 'Bajo',
  MODERADO: 'Moderado',
  ALTO: 'Alto',
  EXTREMO: 'Extremo',
  CRITICO: 'Extremo',
};

const emptyForm = {
  descripcion: '',
  probabilidad: 'MEDIA',
  impacto: 'MEDIO',
  tratamiento: '',
  entidadResponsable: '',
  accionesMitigacion: '',
  fechaAccion: '',
  estado: 'PENDIENTE',
};

const deriveInherentScore = (probability, impact) => {
  const p = String(probability || '').toUpperCase();
  const i = String(impact || '').toUpperCase();
  const pMap = { BAJA: 1, MEDIA: 2, ALTA: 3 };
  const iMap = { BAJO: 1, MEDIO: 2, ALTO: 3 };
  return (pMap[p] || 0) + (iMap[i] || 0);
};

const getRiskLevelFromScore = (score) => {
  if (score >= 2 && score <= 4) return 'BAJO';
  if (score === 5) return 'MODERADO';
  if (score >= 6 && score <= 7) return 'ALTO';
  if (score >= 8 && score <= 10) return 'EXTREMO';
  return 'BAJO';
};

const formatLongText = (value, fallback = '—') => {
  if (value == null || value === '') return fallback;
  return value;
};

const normalizeRiskLevel = (value) => {
  const normalized = String(value || '').trim().toUpperCase();
  if (!normalized) return 'SIN_NIVEL';
  if (normalized === 'CRITICO') return 'EXTREMO';
  return normalized;
};

const formatRiskLevelLabel = (value) => {
  const key = normalizeRiskLevel(value);
  return RISK_LEVEL_LABELS[key] || key.replace(/_/g, ' ');
};

const getProjectRiskKey = (risk) => (
  String(
    risk?.proyectoId
      ?? risk?.proyecto_id
      ?? risk?.codigoProyecto
      ?? risk?.codigo_proyecto
      ?? risk?.proyecto
      ?? ''
  ).toUpperCase()
);

const RiesgosPage = () => {
  const params = useParams();
  const proyectoId = (params.codigoProyecto || params.id || '').toUpperCase();
  const canEdit = usePermission('PROYECTO:EDITAR');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [riskList, setRiskList] = useState([]);
  const [matrix, setMatrix] = useState([]);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [modalOpen, setModalOpen] = useState(false);
  const [solutionModalOpen, setSolutionModalOpen] = useState(false);
  const [solutionRisk, setSolutionRisk] = useState(null);
  const [solutionFiles, setSolutionFiles] = useState([]);
  const [solutionSaving, setSolutionSaving] = useState(false);
  const [solutionError, setSolutionError] = useState(null);
  const [previewSolution, setPreviewSolution] = useState(null);
  const [matrixHelpOpen, setMatrixHelpOpen] = useState(false);
  const [openSelect, setOpenSelect] = useState(null);
  const [evidencias, setEvidencias] = useState([]);
  const [exportingExcel, setExportingExcel] = useState(false);

  const closeModal = () => {
    setModalOpen(false);
    setEditingId(null);
    setForm(emptyForm);
    setOpenSelect(null);
    setEvidencias([]);
  };

  const closeSolutionModal = () => {
    setSolutionModalOpen(false);
    setSolutionRisk(null);
    setSolutionFiles([]);
    setSolutionSaving(false);
    setSolutionError(null);
    setPreviewSolution(null);
  };

  useEffect(() => {
    if (!modalOpen && !matrixHelpOpen && !solutionModalOpen) return undefined;

    const handleKeyDown = (event) => {
      if (event.key !== 'Escape') return;
      if (modalOpen) {
        closeModal();
        return;
      }
      if (solutionModalOpen) {
        closeSolutionModal();
        return;
      }
      setMatrixHelpOpen(false);
    };

    document.body.classList.add('modal-open');
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.classList.remove('modal-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [matrixHelpOpen, modalOpen, solutionModalOpen]);

  const toggleSelect = (name) => {
    setOpenSelect((current) => (current === name ? null : name));
  };

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [riskResp, matrixResp] = await Promise.all([
        riskService.getRiskList(proyectoId),
        riskService.getRiskMatrix(),
      ]);

      const riskData = riskResp?.data?.riesgos ?? riskResp?.data ?? [];
      const matrixData = matrixResp?.data ?? [];

      const normalizedProjectId = String(proyectoId || '').toUpperCase();
      const scopedRiskData = Array.isArray(riskData)
        ? riskData.filter((risk) => {
            const projectKey = getProjectRiskKey(risk);
            return !projectKey || !normalizedProjectId || projectKey === normalizedProjectId;
          })
        : [];

      setRiskList(scopedRiskData);
      setMatrix(Array.isArray(matrixData) ? matrixData : []);
      setError(null);
    } catch (err) {
      console.error(err);
      setError('No fue posible cargar la matriz de riesgos.');
    } finally {
      setLoading(false);
    }
  }, [proyectoId]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchData();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [fetchData]);

  const matrixLookup = useMemo(() => {
    return matrix.reduce((acc, item) => {
      acc[`${item.probabilidad}|${item.impacto}`] = {
        probabilidad: item.probabilidad,
        impacto: item.impacto,
        nivel: normalizeRiskLevel(item.nivel || item.nivelResultante || item.nivel_resultante || 'SIN_NIVEL'),
        color: item.color || item.colorHex || item.color_hex || null,
      };
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
    const counts = { BAJO: 0, MODERADO: 0, ALTO: 0, EXTREMO: 0 };
    riskList.forEach((risk) => {
      const key = normalizeRiskLevel(risk.nivel);
      if (counts[key] !== undefined) counts[key] += 1;
    });
    return counts;
  }, [riskList]);

  const treatmentStats = useMemo(() => {
    return riskList.reduce(
      (acc, risk) => {
        const key = String(risk.estado || 'PENDIENTE').toUpperCase();
        if (key === 'TRATADO') acc.TRATADO += 1;
        else acc.PENDIENTE += 1;
        return acc;
      },
      { TRATADO: 0, PENDIENTE: 0 }
    );
  }, [riskList]);

  const openCreateModal = () => {
    if (!canEdit) return;
    setEditingId(null);
    setForm(emptyForm);
    setModalOpen(true);
  };

  const openMatrixHelp = () => {
    setMatrixHelpOpen(true);
  };

  const openSolutionModal = (risk) => {
    if (!canEdit || !risk) return;
    setSolutionRisk(risk);
    setSolutionFiles([]);
    setSolutionError(null);
    setSolutionModalOpen(true);
  };

  const handleSolutionFiles = (event) => {
    const selectedFiles = Array.from(event.target.files || []);
    if (selectedFiles.length === 0) return;

    const pdfFiles = selectedFiles.filter((file) => {
      const type = String(file.type || '').toLowerCase();
      const name = String(file.name || '').toLowerCase();
      return type === 'application/pdf' || name.endsWith('.pdf');
    });

    const rejected = selectedFiles.length - pdfFiles.length;
    if (rejected > 0) {
      setSolutionError('Solo se permiten archivos PDF.');
    } else {
      setSolutionError(null);
    }

    setSolutionFiles((current) => {
      const existingKeys = new Set(current.map((file) => `${file.name}_${file.size}_${file.lastModified}`));
      const nextFiles = [...current];
      pdfFiles.forEach((file) => {
        const key = `${file.name}_${file.size}_${file.lastModified}`;
        if (!existingKeys.has(key)) {
          existingKeys.add(key);
          nextFiles.push(file);
        }
      });
      return nextFiles;
    });

    event.target.value = '';
  };

  const removePendingSolutionFile = (index) => {
    setSolutionFiles((current) => current.filter((_, currentIndex) => currentIndex !== index));
  };

  const downloadSolutionFile = async (solution) => {
    if (!solution?.id || !solutionRisk?.id) return;
    try {
      const blob = await riskService.downloadRiskSolution(proyectoId, solutionRisk.id, solution.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', solution.nombreOriginal || `solucion-riesgo-${solution.id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setSolutionError('No fue posible descargar el PDF de la solución.');
    }
  };

  const previewSolutionFile = async (solution) => {
    if (!solution?.id || !solutionRisk?.id) return;
    try {
      const blob = await riskService.downloadRiskSolution(proyectoId, solutionRisk.id, solution.id);
      const blobUrl = window.URL.createObjectURL(blob);
      setPreviewSolution({ ...solution, blobUrl });
    } catch (err) {
      console.error(err);
      setSolutionError('No fue posible cargar la vista previa del PDF.');
    }
  };

  const handleSolutionSubmit = async (event) => {
    event.preventDefault();
    if (!canEdit || !solutionRisk) return;

    try {
      setSolutionSaving(true);
      setSolutionError(null);
      await riskService.uploadRiskSolutions(proyectoId, solutionRisk.id, solutionFiles);
      closeSolutionModal();
      await fetchData();
    } catch (err) {
      console.error(err);
      setSolutionError(err?.response?.data?.detail || 'No fue posible cargar las soluciones.');
    } finally {
      setSolutionSaving(false);
    }
  };

  const formatBytes = (bytes) => {
    if (!bytes && bytes !== 0) return 'N/A';
    const size = Number(bytes);
    if (Number.isNaN(size)) return 'N/A';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  };

  const downloadRiskMatrixExcel = async () => {
    if (exportingExcel) return;
    try {
      setExportingExcel(true);
      const blob = await riskService.downloadRiskMatrixExcel(proyectoId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Matriz de Riesgos ${proyectoId || 'proyecto'}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setError('No fue posible generar el Excel de la matriz de riesgos.');
    } finally {
      setExportingExcel(false);
    }
  };

  const startEdit = (risk) => {
    if (!canEdit) return;
    setEditingId(risk.id);
    setForm({
      descripcion: risk.descripcion || '',
      probabilidad: risk.probabilidad || 'MEDIA',
      impacto: risk.impacto || 'MEDIO',
      tratamiento: risk.tratamiento || '',
      entidadResponsable: risk.entidadResponsable || '',
      accionesMitigacion: risk.accionesMitigacion || '',
      fechaAccion: risk.fechaAccion || '',
      estado: risk.estado || 'PENDIENTE',
    });
    setModalOpen(true);
  };

  const handleEvidenciasChange = (e) => {
    const files = Array.from(e.target.files || []);
    const pdfFiles = files.filter((f) => f.type === 'application/pdf');
    setEvidencias((prev) => [...prev, ...pdfFiles].slice(0, 10));
    e.target.value = '';
  };

  const removeEvidencia = (index) => {
    setEvidencias((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canEdit) return;

    try {
      setSaving(true);
      const payload = {
        descripcion: form.descripcion,
        probabilidad: form.probabilidad,
        impacto: form.impacto,
        tratamiento: form.tratamiento,
        entidadResponsable: form.entidadResponsable,
        accionesMitigacion: form.accionesMitigacion,
        fechaAccion: form.fechaAccion || null,
        estado: form.estado,
      };

      let riskId = editingId;
      if (editingId) {
        await riskService.updateRisk(proyectoId, editingId, payload);
      } else {
        const created = await riskService.createRisk(proyectoId, payload);
        riskId = created?.data?.id ?? created?.data?.data?.id;
      }

      if (riskId && evidencias.length > 0) {
        await riskService.uploadRiskSolutions(proyectoId, riskId, evidencias);
      }

      closeModal();
      await fetchData();
    } catch (err) {
      setError(err?.response?.data?.detail || 'No fue posible guardar el riesgo.');
    } finally {
      setSaving(false);
    }
  };

  const removeRisk = async (riskId) => {
    if (!canEdit) return;
    if (!window.confirm('¿Eliminar este riesgo?')) return;
    await riskService.deleteRisk(proyectoId, riskId);
    await fetchData();
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
            Vista resumida para identificar, valorar y tratar riesgos. El detalle completo se captura en un modal
            para evitar una pantalla saturada y facilitar la lectura.
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
          <button className="btn-help" onClick={openMatrixHelp} type="button">
            <Info size={16} /> Cómo leer la matriz
          </button>
          <button className="btn-secondary" onClick={downloadRiskMatrixExcel} type="button" disabled={exportingExcel}>
            <Download size={16} /> {exportingExcel ? 'Generando Excel...' : 'Descargar Excel'}
          </button>
          <button className="btn-primary btn-add-risk" onClick={openCreateModal} type="button" disabled={!canEdit}>
            <Plus size={16} /> Agregar riesgo
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
        <div className="risk-stat extremo">
          <span>Extremo</span>
          <strong>{stats.EXTREMO}</strong>
        </div>
      </div>

      <section className="panel risks-table-panel">
        <div className="panel-title">
          <div>
            <h2>Registro de riesgos</h2>
            <span className="panel-subtitle">Listado resumido, acciones en modal y niveles claros</span>
          </div>
          <span className="panel-chip">{riskList.length} registros</span>
        </div>

        <div className="wide-table-wrap">
          <table className="risk-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Descripción</th>
                <th>Probabilidad</th>
                <th>Impacto</th>
                <th>Calificación</th>
                <th>Nivel</th>
                <th>Como mitigar</th>
                <th>Responsable</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {riskList.length === 0 ? (
                <tr>
                  <td colSpan={10}>
                    <div className="empty-box">No hay riesgos registrados para este proyecto.</div>
                  </td>
                </tr>
              ) : (
                riskList.map((risk, index) => {
                  const score = deriveInherentScore(risk.probabilidad, risk.impacto);
                  const level = normalizeRiskLevel(risk.nivel || getRiskLevelFromScore(score));

                  return (
                    <tr key={risk.id}>
                      <td>{index + 1}</td>
                      <td className="risk-cell-left">
                        <span>{formatLongText(risk.descripcion)}</span>
                      </td>
                      <td>
                        <span className="code-pill">{risk.probabilidad || '—'}</span>
                      </td>
                      <td>
                        <span className="code-pill">{risk.impacto || '—'}</span>
                      </td>
                      <td>
                        <span className="code-pill strong">{score || '—'}</span>
                      </td>
                      <td>
                        <span className={`risk-badge ${level.toLowerCase()}`}>{formatRiskLevelLabel(level)}</span>
                      </td>
                      <td className="risk-cell-left">
                        <span>{formatLongText(risk.tratamiento)}</span>
                      </td>
                      <td className="risk-cell-left">
                        <span>{formatLongText(risk.entidadResponsable)}</span>
                      </td>
                      <td>
                        <span className={`risk-badge ${String(risk.estado || 'PENDIENTE').toLowerCase()}`}>
                          {risk.estado || 'PENDIENTE'}
                        </span>
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="btn-solution compact"
                            onClick={() => openSolutionModal(risk)}
                            disabled={!canEdit}
                            title="Dar solución"
                          >
                            <FileUp size={14} />
                          </button>
                          <button
                            type="button"
                            className="btn-secondary compact"
                            onClick={() => startEdit(risk)}
                            disabled={!canEdit}
                          >
                            <Pencil size={14} />
                          </button>
                          <button
                            type="button"
                            className="btn-danger compact"
                            onClick={() => removeRisk(risk.id)}
                            disabled={!canEdit}
                          >
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

      {modalOpen && (
        <div className="risk-modal-overlay" role="presentation" onClick={closeModal}>
          <div className="risk-modal" role="dialog" aria-modal="true" aria-labelledby="risk-modal-title" onClick={(event) => event.stopPropagation()}>
            <div className="risk-modal-header">
              <div>
                <span className="panel-chip">{editingId ? 'Editar riesgo' : 'Nuevo riesgo'}</span>
                <h2 id="risk-modal-title">{editingId ? 'Editar riesgo' : 'Agregar riesgo'}</h2>
                <p>Completa los campos clave. La matriz debe poder leerse sin cargar la pantalla completa.</p>
              </div>
              <button type="button" className="modal-close-btn" onClick={closeModal} aria-label="Cerrar">
                <X size={18} />
              </button>
            </div>

            <form className="risk-form risk-form-modal" onSubmit={handleSubmit}>
              <label>
                Descripción del riesgo
                <SpellCheckerTextarea
                  value={form.descripcion}
                  onChange={(e) => setForm((prev) => ({ ...prev, descripcion: e.target.value }))}
                  required
                  rows={3}
                  placeholder="Describe el evento o condición de riesgo de forma concreta."
                />
              </label>

              <div className="form-grid form-grid-2">
                <label>
                  Probabilidad
                  <AutocompleteSelect
                    value={form.probabilidad}
                    onChange={(val) => setForm((prev) => ({ ...prev, probabilidad: val }))}
                    options={matrixProbabilidades.map((item) => ({ value: item, label: item }))}
                    placeholder="Seleccionar..."
                    sortAlphabetically={false}
                  />
                </label>
                <label>
                  Impacto
                  <AutocompleteSelect
                    value={form.impacto}
                    onChange={(val) => setForm((prev) => ({ ...prev, impacto: val }))}
                    options={matrixImpactos.map((item) => ({ value: item, label: item }))}
                    placeholder="Seleccionar..."
                    sortAlphabetically={false}
                  />
                </label>
              </div>

              <label>
                Cómo mitigar el riesgo
                <SpellCheckerTextarea
                  value={form.tratamiento}
                  onChange={(e) => setForm((prev) => ({ ...prev, tratamiento: e.target.value }))}
                  rows={3}
                  placeholder="Plan de tratamiento: aceptar, mitigar, transferir o evitar."
                />
              </label>

              <label>
                Responsable
                <SpellCheckerInput
                  value={form.entidadResponsable}
                  onChange={(e) => setForm((prev) => ({ ...prev, entidadResponsable: e.target.value }))}
                  placeholder="Secretaría TIC, despacho, gerente del proyecto..."
                />
              </label>

              <label>
                Acciones realizadas para mitigar el riesgo
                <SpellCheckerTextarea
                  value={form.accionesMitigacion}
                  onChange={(e) => setForm((prev) => ({ ...prev, accionesMitigacion: e.target.value }))}
                  rows={3}
                  placeholder="Acciones concretas para reducir probabilidad o impacto."
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
                Estado del riesgo
                <AutocompleteSelect
                  value={form.estado}
                  onChange={(val) => setForm((prev) => ({ ...prev, estado: val }))}
                  options={ESTADOS.map((item) => ({ value: item, label: item }))}
                  placeholder="Seleccionar estado..."
                  sortAlphabetically={false}
                />
              </label>

              {form.estado === 'TRATADO' && (
                <label className="file-picker">
                  <span>Cargar evidencias (PDF)</span>
                  <input
                    type="file"
                    accept=".pdf,application/pdf"
                    multiple
                    onChange={handleEvidenciasChange}
                  />
                  {evidencias.length > 0 && (
                    <div className="pending-files">
                      {evidencias.map((file, idx) => (
                        <article className="pending-file" key={`${file.name}_${file.size}_${idx}`}>
                          <div className="solution-item-copy">
                            <strong>{file.name}</strong>
                            <span>{formatBytes(file.size)}</span>
                          </div>
                          <button
                            type="button"
                            className="btn-danger compact"
                            onClick={() => removeEvidencia(idx)}
                            title="Quitar archivo"
                          >
                            <Trash2 size={14} />
                          </button>
                        </article>
                      ))}
                    </div>
                  )}
                </label>
              )}

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>
                  Cancelar
                </button>
                <button className="btn-primary" disabled={!canEdit || saving} type="submit">
                  <Save size={16} /> {editingId ? 'Guardar cambios' : 'Crear riesgo'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {solutionModalOpen && (
        <div className="solution-modal-overlay" role="presentation" onClick={closeSolutionModal}>
          <div
            className="solution-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="solution-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="risk-modal-header">
              <div>
                <span className="panel-chip">Dar solución</span>
                <h2 id="solution-modal-title">{solutionRisk?.descripcion || 'Riesgo seleccionado'}</h2>
                <p>
                  Adjunta uno o varios PDF para este riesgo. Los archivos nuevos se suman a los ya cargados sin
                  borrar los anteriores.
                </p>
              </div>
              <button type="button" className="modal-close-btn" onClick={closeSolutionModal} aria-label="Cerrar">
                <X size={18} />
              </button>
            </div>

            {solutionError && (
              <div className="riesgos-alert solution-alert">
                <AlertTriangle size={16} />
                {solutionError}
              </div>
            )}

            <div className="solution-modal-grid">
              <section className="solution-panel">
                <div className="panel-title compact">
                  <div>
                    <h3>Soluciones cargadas</h3>
                    <span className="panel-subtitle">
                      {solutionRisk?.soluciones?.length || 0} archivo{(solutionRisk?.soluciones?.length || 0) === 1 ? '' : 's'}
                    </span>
                  </div>
                </div>

                <div className="solution-list">
                  {(solutionRisk?.soluciones || []).length === 0 ? (
                    <div className="empty-box">Todavía no hay PDFs asociados a este riesgo.</div>
                  ) : (
                    solutionRisk.soluciones.map((solution) => (
                      <article className="solution-item" key={solution.id}>
                        <div className="solution-item-copy">
                          <strong>{solution.nombreOriginal}</strong>
                          <span>
                            {formatBytes(solution.tamanoBytes)} · {solution.fechaCarga ? new Date(solution.fechaCarga).toLocaleString() : 'Sin fecha'}
                          </span>
                        </div>
                        <div className="solution-item-actions">
                          <button
                            type="button"
                            className="btn-secondary compact"
                            onClick={() => previewSolutionFile(solution)}
                            title="Vista previa"
                          >
                            <Eye size={14} />
                          </button>
                          <button
                            type="button"
                            className="btn-secondary compact"
                            onClick={() => downloadSolutionFile(solution)}
                            title="Descargar PDF"
                          >
                            <Download size={14} />
                          </button>
                        </div>
                      </article>
                    ))
                  )}
                </div>
              </section>

              <section className="solution-panel">
                <div className="panel-title compact">
                  <div>
                    <h3>Agregar PDFs</h3>
                    <span className="panel-subtitle">Selecciona uno o varios archivos</span>
                  </div>
                </div>

                <form className="risk-form solution-form" onSubmit={handleSolutionSubmit}>
                  <label className="file-picker">
                    <span>Archivos PDF</span>
                    <input
                      type="file"
                      accept=".pdf,application/pdf"
                      multiple
                      onChange={handleSolutionFiles}
                    />
                  </label>

                  <div className="pending-files">
                    {solutionFiles.length === 0 ? (
                      <div className="empty-box">Aún no has seleccionado archivos.</div>
                    ) : (
                      solutionFiles.map((file, index) => (
                        <article className="pending-file" key={`${file.name}_${file.size}_${file.lastModified}`}>
                          <div className="solution-item-copy">
                            <strong>{file.name}</strong>
                            <span>{formatBytes(file.size)}</span>
                          </div>
                          <button
                            type="button"
                            className="btn-danger compact"
                            onClick={() => removePendingSolutionFile(index)}
                            title="Quitar archivo"
                          >
                            <Trash2 size={14} />
                          </button>
                        </article>
                      ))
                    )}
                  </div>

                  <div className="form-actions">
                    <button type="button" className="btn-secondary" onClick={closeSolutionModal}>
                      Cancelar
                    </button>
                    <button className="btn-primary" disabled={!canEdit || solutionSaving || solutionFiles.length === 0} type="submit">
                      <Save size={16} /> {solutionSaving ? 'Guardando...' : 'Guardar soluciones'}
                    </button>
                  </div>
                </form>
              </section>
            </div>
          </div>
        </div>
      )}

      {previewSolution && solutionRisk && (
        <div className="pdf-preview-backdrop" role="presentation" onClick={() => { window.URL.revokeObjectURL(previewSolution.blobUrl); setPreviewSolution(null); }}>
          <div className="pdf-preview-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <div className="pdf-preview-header">
              <span>{previewSolution.nombreOriginal}</span>
              <button type="button" className="modal-close-btn" onClick={() => { window.URL.revokeObjectURL(previewSolution.blobUrl); setPreviewSolution(null); }} aria-label="Cerrar">
                <X size={18} />
              </button>
            </div>
            <iframe
              className="pdf-preview-iframe"
              src={previewSolution.blobUrl}
              title={previewSolution.nombreOriginal}
            />
          </div>
        </div>
      )}

      {matrixHelpOpen && (
        <div className="matrix-help-overlay" role="presentation" onClick={() => setMatrixHelpOpen(false)}>
          <div
            className="matrix-help-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="matrix-help-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="matrix-help-header">
              <div>
                <span className="panel-chip">Referencia rápida</span>
                <h2 id="matrix-help-title">Cómo funciona la matriz de riesgos</h2>
                <p>
                  La matriz traduce probabilidad e impacto en un nivel de riesgo estandarizado. El color ayuda a
                  leer la severidad de un vistazo y el control siempre debe bajar el residual.
                </p>
                <div className="matrix-help-summary">
                  <span className="summary-pill success">Tratados: {treatmentStats.TRATADO}</span>
                  <span className="summary-pill warning">Pendientes: {treatmentStats.PENDIENTE}</span>
                </div>
              </div>
              <button type="button" className="modal-close-btn" onClick={() => setMatrixHelpOpen(false)} aria-label="Cerrar">
                <X size={18} />
              </button>
            </div>

            <div className="matrix-help-content">
              <section className="matrix-help-notes">
                <article>
                  <span>1. Probabilidad</span>
                  <strong>Qué tan posible es que ocurra el evento.</strong>
                </article>
                <article>
                  <span>2. Impacto</span>
                  <strong>Qué tan grave sería si el riesgo ocurre.</strong>
                </article>
                <article>
                  <span>3. Nivel resultante</span>
                  <strong>El sistema cruza ambos valores y asigna BAJO, MODERADO, ALTO o EXTREMO.</strong>
                </article>
                <article>
                  <span>4. Residual</span>
                  <strong>Después de controles, el nivel esperado debe bajar si la mitigación funciona.</strong>
                </article>
              </section>

              <section className="matrix-help-sample">
                <div className="panel-title compact">
                  <div>
                    <h3>Lectura visual</h3>
                    <span className="panel-subtitle">Filas = probabilidad, columnas = impacto</span>
                  </div>
                  <span className="panel-chip">{matrix.length} cruces</span>
                </div>
                <div className="matrix-grid-preview matrix-grid-preview-modal">
                  <div className="matrix-grid-header empty"></div>
                  {matrixImpactos.map((impacto) => (
                    <div key={impacto} className="matrix-grid-header">
                      {impacto}
                    </div>
                  ))}
                  {matrixRows.map((row) => (
                    <Fragment key={row.prob}>
                      <div className="matrix-grid-header row">{row.prob}</div>
                      {row.cells.map((cell, index) => (
                        <div
                          key={`${row.prob}-${matrixImpactos[index]}`}
                          className={`matrix-grid-cell ${cell?.nivel ? normalizeRiskLevel(cell.nivel).toLowerCase() : 'vacio'}`}
                          style={cell?.color ? { borderColor: cell.color } : undefined}
                        >
                          {cell ? (
                            <>
                              <strong>{formatRiskLevelLabel(cell.nivel)}</strong>
                              <span>
                                {cell.probabilidad} · {cell.impacto}
                              </span>
                            </>
                          ) : (
                            <span>Sin dato</span>
                          )}
                        </div>
                      ))}
                    </Fragment>
                  ))}
                </div>
              </section>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default RiesgosPage;
