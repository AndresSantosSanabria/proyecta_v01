import { useEffect, useState } from 'react';
import {
  Plus, Trash2, Save, Eye, Type, Table2,
  LoaderCircle, ChevronDown, ChevronUp, CheckCircle2,
  Columns3, Signature, AlignLeft, Heading1,
} from 'lucide-react';
import securityService from '../../services/securityService';
import SpellCheckerInput from '../common/SpellCheckerInput';
import { AutocompleteSelect } from '../common/AutocompleteSelect';
import './ClosureTemplatePanel.css';

const COMPONENT_TYPES = [
  { type: 'section', label: 'Encabezado de Seccion', desc: 'Titulo con estilo enriquescido para separar bloques de contenido.', icon: Heading1 },
  { type: 'field', label: 'Campo de Datos', desc: 'Par de etiqueta + entrada de texto para capturar informacion especifica.', icon: Type },
  { type: 'table', label: 'Tabla de Filas Multiples', desc: 'Tabla con columnas configurables para listados y matrices.', icon: Columns3 },
  { type: 'textarea', label: 'Bloque de Texto', desc: 'Area para descripciones extensas, resumenes o notas.', icon: AlignLeft },
  { type: 'signature', label: 'Bloque de Firma', desc: 'Area de validacion digital para responsables y aprobadores.', icon: Signature },
];

const uid = () => `id_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

const createComponent = (type) => {
  switch (type) {
    case 'section':
      return { id: uid(), type: 'section', titulo: 'Nueva Seccion', orden: 0, activo: true };
    case 'field':
      return { id: uid(), type: 'field', label: 'Campo', tipo_input: 'texto_corto', questionId: null, activo: true, readonly: false };
    case 'table':
      return { id: uid(), type: 'table', titulo: 'Tabla', activo: true, columnas: [{ id: uid(), label: 'Columna 1', activo: true, orden: 1 }] };
    case 'textarea':
      return { id: uid(), type: 'textarea', label: 'Texto largo', tipo_input: 'texto_largo', questionId: null, activo: true, readonly: false };
    case 'signature':
      return { id: uid(), type: 'signature', label: 'Firma', questionId: null, activo: true, readonly: false };
    default:
      return null;
  }
};

const ClosureTemplatePanel = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState(null);
  const [metadata, setMetadata] = useState({ codigo_proceso: 'A-GT-FR-004', nombre_documento: 'Acta de Cierre de Proyecto', version_num: 1 });
  const [blocks, setBlocks] = useState([]);
  const [expandedBlock, setExpandedBlock] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [previewOpen, setPreviewOpen] = useState(false);

  useEffect(() => { loadTemplate(); loadQuestions(); }, []);

  const getBackendError = (err) => {
    const data = err.response?.data;
    if (data?.detail) return data.detail;
    if (data?.message) return data.message;
    if (typeof data === 'string') return data;
    return 'No se pudo guardar la plantilla.';
  };

  const loadTemplate = async () => {
    try {
      setLoading(true);
      const res = await securityService.getActiveClosureTemplate();
      const data = res?.data?.data ?? res?.data ?? res;
      if (data) {
        setMetadata({
          codigo_proceso: data.codigo_proceso || 'A-GT-FR-004',
          nombre_documento: data.nombre_documento || 'Acta de Cierre de Proyecto',
          version_num: data.version_num || 1,
        });
        const tpl = data.template_json || data;
        if (tpl.secciones && Array.isArray(tpl.secciones)) {
          const flat = [];
          tpl.secciones.forEach((sec) => {
            flat.push({ id: uid(), type: 'section', titulo: sec.titulo || '', orden: sec.orden || 0, activo: sec.activo !== false });
            if (sec.tipo_seccion === 'formulario' && sec.campos) {
              sec.campos.forEach((c) => {
                flat.push({
                  id: uid(),
                  type: c.tipo_input === 'texto_largo' ? 'textarea' : 'field',
                  label: c.label || '',
                  tipo_input: c.tipo_input || 'texto_corto',
                  questionId: c.questionId || null,
                  activo: c.activo !== false,
                  readonly: Boolean(c.readonly || c.questionId),
                });
              });
            }
            if (sec.tipo_seccion === 'tabla' && sec.columnas) {
              flat.push({
                id: uid(),
                type: 'table',
                titulo: sec.titulo || '',
                activo: sec.activo !== false,
                columnas: sec.columnas.map((col, idx) => ({
                  id: uid(),
                  label: col.label,
                  activo: col.activo !== false,
                  orden: col.orden || (idx + 1),
                })),
              });
            }
          });
          setBlocks(flat);
        }
      }
    } catch (err) {
      console.error('Error loading template:', err);
      setError(getBackendError(err));
    } finally {
      setLoading(false);
    }
  };

  const loadQuestions = async () => {
    try {
      const res = await securityService.listClosureQuestions();
      const data = res?.data?.data ?? res?.data ?? res;
      setQuestions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error loading questions:', err);
    }
  };

  const addBlock = (type) => {
    const block = createComponent(type);
    if (!block) return;
    setBlocks((prev) => [...prev, block]);
    setExpandedBlock(block.id);
  };

  const removeBlock = (idx) => {
    setBlocks((prev) => prev.filter((_, i) => i !== idx));
    setExpandedBlock(null);
  };

  const updateBlock = (idx, updates) => {
    setBlocks((prev) => prev.map((b, i) => (i === idx ? { ...b, ...updates } : b)));
  };

  const moveBlock = (idx, dir) => {
    setBlocks((prev) => {
      const arr = [...prev];
      const ni = idx + dir;
      if (ni < 0 || ni >= arr.length) return arr;
      [arr[idx], arr[ni]] = [arr[ni], arr[idx]];
      return arr;
    });
  };

  const addColumn = (idx) => {
    setBlocks((prev) => prev.map((b, i) => {
      if (i !== idx || b.type !== 'table') return b;
      return {
        ...b,
        columnas: [...(b.columnas || []), {
          id: uid(),
          label: `Columna ${(b.columnas || []).length + 1}`,
          activo: true,
          orden: (b.columnas || []).length + 1,
        }],
      };
    }));
  };

  const updateColumn = (blockIdx, colIdx, label) => {
    setBlocks((prev) => prev.map((b, i) => {
      if (i !== blockIdx || b.type !== 'table') return b;
      return { ...b, columnas: b.columnas.map((c, ci) => (ci === colIdx ? { ...c, label } : c)) };
    }));
  };

  const removeColumn = (blockIdx, colIdx) => {
    setBlocks((prev) => prev.map((b, i) => {
      if (i !== blockIdx || b.type !== 'table') return b;
      return { ...b, columnas: b.columnas.filter((_, ci) => ci !== colIdx) };
    }));
  };

  const buildTemplateJson = () => {
    const secciones = [];
    let currentSection = null;
    let sectionIdx = 0;
    let fieldIdx = 0;

    for (const block of blocks) {
      if (block.type === 'section') {
        if (currentSection) secciones.push(currentSection);
        sectionIdx++;
        currentSection = {
          id: `seccion_${sectionIdx}`,
          titulo: block.titulo,
          orden: sectionIdx,
          tipo_seccion: 'formulario',
          campos: [],
          columnas: [],
          activo: block.activo !== false,
        };
        fieldIdx = 0;
      } else if (currentSection) {
        if (block.type === 'field' || block.type === 'textarea') {
          fieldIdx += 1;
          const campo = {
            id: block.id,
            label: block.label,
            tipo_input: block.tipo_input || 'texto_corto',
            activo: block.activo !== false,
            orden: fieldIdx,
          };
          if (block.questionId) campo.questionId = block.questionId;
          if (block.readonly) campo.readonly = true;
          currentSection.campos.push(campo);
        } else if (block.type === 'table') {
          currentSection.tipo_seccion = 'tabla';
          currentSection.columnas = (block.columnas || []).map((c, index) => ({
            id: c.id,
            label: c.label,
            activo: c.activo !== false,
            orden: c.orden || (index + 1),
          }));
        } else if (block.type === 'signature') {
          fieldIdx += 1;
          const campo = {
            id: block.id,
            label: block.label,
            tipo_input: 'texto_corto',
            activo: block.activo !== false,
            orden: fieldIdx,
          };
          if (block.questionId) campo.questionId = block.questionId;
          if (block.readonly) campo.readonly = true;
          currentSection.campos.push(campo);
        }
      }
    }
    if (currentSection) secciones.push(currentSection);
    return { secciones };
  };

  const handleSave = async () => {
    if (!metadata.codigo_proceso.trim() || !metadata.nombre_documento.trim()) {
      setError('El codigo de proceso y nombre del documento son obligatorios.');
      return;
    }
    const sections = blocks.filter((b) => b.type === 'section');
    if (sections.length === 0) {
      setError('Debe agregar al menos una seccion.');
      return;
    }
    for (const sec of sections) {
      if (!sec.titulo.trim()) {
        setError('Todos los encabezados deben tener un titulo.');
        return;
      }
    }
    const fieldBlocks = blocks.filter((b) => b.type === 'field' || b.type === 'textarea' || b.type === 'signature');
    for (const fb of fieldBlocks) {
      if (!fb.label?.trim()) {
        setError('Todos los campos deben tener un nombre (label).');
        return;
      }
    }
    const tableBlocks = blocks.filter((b) => b.type === 'table');
    for (const tb of tableBlocks) {
      if (!tb.columnas || tb.columnas.length === 0) {
        setError('Las tablas deben tener al menos una columna.');
        return;
      }
      for (const col of tb.columnas) {
        if (!col.label?.trim()) {
          setError('Todas las columnas de las tablas deben tener un nombre.');
          return;
        }
      }
    }
    try {
      setSaving(true);
      setError(null);
      setNotice(null);
      await securityService.saveClosureTemplate({
        codigo_proceso: metadata.codigo_proceso,
        nombre_documento: metadata.nombre_documento,
        template_json: buildTemplateJson(),
      });
      setNotice('Plantilla guardada exitosamente.');
      await loadTemplate();
      setTimeout(() => setNotice(null), 3000);
    } catch (err) {
      setError(getBackendError(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="ctp-loading">
        <LoaderCircle size={20} className="animate-spin" />
        <span>Cargando plantilla...</span>
      </div>
    );
  }

  return (
    <div className="ctp">
      <div className="ctp-header">
        <div className="ctp-header-left">
          <div className="ctp-meta-top">
            <span className="ctp-eyebrow">Configuracion de Plantilla de Acta de Cierre</span>
            <div className="ctp-title-row">
              <SpellCheckerInput
                className="ctp-title-input"
                value={metadata.nombre_documento}
                onChange={(e) => setMetadata((m) => ({ ...m, nombre_documento: e.target.value }))}
              />
            </div>
            <div className="ctp-meta-row">
              <div className="ctp-meta-item">
                <span className="ctp-meta-label">Codigo del Proceso</span>
                <SpellCheckerInput className="ctp-meta-value-input" value={metadata.codigo_proceso} onChange={(e) => setMetadata((m) => ({ ...m, codigo_proceso: e.target.value }))} />
              </div>
              <div className="ctp-meta-item">
                <span className="ctp-meta-label">Version Actual</span>
                <span className="ctp-meta-value">v{metadata.version_num}.0.0</span>
              </div>
            </div>
          </div>
        </div>
        <div className="ctp-header-actions">
          <button type="button" className="ctp-btn ctp-btn-preview" onClick={() => setPreviewOpen(true)}>
            <Eye size={15} /> Vista Previa
          </button>
          <button type="button" className="ctp-btn ctp-btn-save" onClick={handleSave} disabled={saving}>
            {saving ? <LoaderCircle size={15} className="animate-spin" /> : <Save size={15} />}
            Guardar Cambios
          </button>
        </div>
      </div>

      {notice && <div className="ctp-notice"><CheckCircle2 size={15} /> {notice}</div>}
      {error && <div className="ctp-error">{error}</div>}

      <div className="ctp-body">
        <div className="ctp-canvas">
          <h3 className="ctp-section-title">Estructura del Documento</h3>

          {blocks.map((block, idx) => {
            if (block.type === 'section') {
              return (
                <div key={block.id} className="ctp-block ctp-section-block">
                  <div className="ctp-section-header">
                    <div className="ctp-section-drag">
                      <button type="button" onClick={() => moveBlock(idx, -1)} disabled={idx === 0}><ChevronUp size={14} /></button>
                      <button type="button" onClick={() => moveBlock(idx, 1)} disabled={idx === blocks.length - 1}><ChevronDown size={14} /></button>
                    </div>
                    <span className="ctp-section-num">{idx + 1}</span>
                    <SpellCheckerInput
                      className="ctp-section-title-input"
                      value={block.titulo}
                      onChange={(e) => updateBlock(idx, { titulo: e.target.value })}
                      placeholder="Titulo de la seccion"
                    />
                    <label className="ctp-toggle-inline" title="Activar o desactivar esta seccion">
                      <input
                        type="checkbox"
                        checked={block.activo !== false}
                        onChange={(e) => updateBlock(idx, { activo: e.target.checked })}
                      />
                      <span>{block.activo !== false ? 'Activa' : 'Inactiva'}</span>
                    </label>
                    <button type="button" className="ctp-btn-icon ctp-btn-danger" onClick={() => removeBlock(idx)}>
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              );
            }

            return (
              <div key={block.id} className="ctp-block ctp-component-block">
                <div className="ctp-component-header">
                  <div className="ctp-component-drag">
                    <button type="button" onClick={() => moveBlock(idx, -1)} disabled={idx === 0}><ChevronUp size={12} /></button>
                    <button type="button" onClick={() => moveBlock(idx, 1)} disabled={idx === blocks.length - 1}><ChevronDown size={12} /></button>
                  </div>

                  {(block.type === 'field' || block.type === 'textarea') && (
                    <div className="ctp-component-body ctp-field-editor">
                      <div className="ctp-chip">
                        {block.questionId && <span className="ctp-chip-linked">VINCULADO</span>}
                      </div>
                      <div className="ctp-field-row">
                        <SpellCheckerInput className="ctp-field-label-input" value={block.label} onChange={(e) => updateBlock(idx, { label: e.target.value })} placeholder="Nombre del campo" disabled={!!block.questionId} />
                        <label className="ctp-toggle-inline" title="Activar o desactivar este campo">
                          <input
                            type="checkbox"
                            checked={block.activo !== false}
                            onChange={(e) => updateBlock(idx, { activo: e.target.checked })}
                          />
                          <span>{block.activo !== false ? 'Activo' : 'Inactivo'}</span>
                        </label>
                      </div>
                      <div className="ctp-field-question">
                        <label className="ctp-question-label">Pregunta del Banco (variable dinamica)</label>
                        <AutocompleteSelect
                          className="ctp-question-select"
                          value={block.questionId || ''}
                          onChange={(val) => {
                            const qId = val ? Number(val) : null;
                            const q = questions.find((q) => q.id === qId);
                            const tipoMap = {
                              texto_libre: 'texto_largo',
                              seleccion_unica: 'select',
                              seleccion_multiple: 'select',
                              fecha: 'fecha',
                              numero: 'numero',
                            };
                            updateBlock(idx, {
                              questionId: qId,
                              label: q ? q.texto : block.label,
                              tipo_input: q ? (tipoMap[q.tipo_respuesta] || 'texto_corto') : block.tipo_input,
                            });
                          }}
                          options={questions.filter((q) => q.activo).map((q) => ({
                            value: q.id,
                            label: q.texto,
                          }))}
                          placeholder="Buscar pregunta..."
                          allLabel="Sin vinculo ( campo manual )"
                          allValue=""
                        />
                        {block.questionId && (
                          <span className="ctp-question-hint">El valor se resuelve desde la respuesta de esta pregunta</span>
                        )}
                      </div>
                    </div>
                  )}

                  {block.type === 'table' && (
                    <div className="ctp-component-body ctp-table-editor">
                      <div className="ctp-chip">
                        <Table2 size={12} /> TABLA DINAMICA
                        <span className="ctp-chip-linked" style={{ marginLeft: '8px' }}>{block.activo !== false ? 'ACTIVA' : 'INACTIVA'}</span>
                      </div>
                      <div className="ctp-table-columns">
                        {(block.columnas || []).map((col, ci) => (
                          <div key={col.id} className="ctp-col-chip">
                            <SpellCheckerInput value={col.label} onChange={(e) => updateColumn(idx, ci, e.target.value)} className="ctp-col-chip-input" />
                            <label className="ctp-col-toggle" title="Activar o desactivar columna">
                              <input
                                type="checkbox"
                                checked={col.activo !== false}
                                onChange={(e) => {
                                  const checked = e.target.checked;
                                  setBlocks((prev) => prev.map((b, bi) => {
                                    if (bi !== idx || b.type !== 'table') return b;
                                    return {
                                      ...b,
                                      columnas: b.columnas.map((item, itemIdx) => (itemIdx === ci ? { ...item, activo: checked } : item)),
                                    };
                                  }));
                                }}
                              />
                              <span>{col.activo !== false ? 'Activa' : 'Inactiva'}</span>
                            </label>
                            <button type="button" className="ctp-col-remove" onClick={() => removeColumn(idx, ci)}><Trash2 size={11} /></button>
                          </div>
                        ))}
                        <button type="button" className="ctp-col-add" onClick={() => addColumn(idx)}><Plus size={12} /> Anadir Columna</button>
                      </div>
                    </div>
                  )}

                  {block.type === 'signature' && (
                    <div className="ctp-component-body ctp-signature-editor">
                      <div className="ctp-chip"><Signature size={12} /> BLOQUE DE FIRMA</div>
                      <SpellCheckerInput className="ctp-field-label-input" value={block.label} onChange={(e) => updateBlock(idx, { label: e.target.value })} placeholder="Nombre del firmante" />
                      <label className="ctp-toggle-inline" title="Activar o desactivar este bloque">
                        <input
                          type="checkbox"
                          checked={block.activo !== false}
                          onChange={(e) => updateBlock(idx, { activo: e.target.checked })}
                        />
                        <span>{block.activo !== false ? 'Activo' : 'Inactivo'}</span>
                      </label>
                    </div>
                  )}

                  <button type="button" className="ctp-btn-icon ctp-btn-danger" onClick={() => removeBlock(idx)}>
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>
            );
          })}

          <div className="ctp-dropzone">
            <Plus size={24} />
            <span>Arrastra un componente aqui para anadirlo</span>
            <span className="ctp-dropzone-hint">Usa la barra lateral derecha para agregar componentes</span>
          </div>
        </div>

        <div className="ctp-sidebar">
          <h3 className="ctp-sidebar-title">Libreria de Componentes</h3>
          <div className="ctp-sidebar-list">
            {COMPONENT_TYPES.map((comp) => {
              const Icon = comp.icon;
              return (
                <button key={comp.type} type="button" className="ctp-sidebar-item" onClick={() => addBlock(comp.type)}>
                  <div className="ctp-sidebar-item-icon"><Icon size={18} /></div>
                  <div className="ctp-sidebar-item-text">
                    <strong>{comp.label}</strong>
                    <span>{comp.desc}</span>
                  </div>
                </button>
              );
            })}
          </div>
          <div className="ctp-sidebar-tip">
            <strong>Consejo de Usuario</strong>
            <p>Puedes arrastrar y soltar los elementos directamente en el canvas central para construir tu documento.</p>
          </div>
        </div>
      </div>

      {previewOpen && (
        <div className="ctp-preview-overlay" onClick={() => setPreviewOpen(false)}>
          <div className="ctp-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ctp-preview-header">
              <h3>Vista Previa del Documento</h3>
              <button type="button" className="ctp-preview-close" onClick={() => setPreviewOpen(false)}>&times;</button>
            </div>
            <div className="ctp-preview-body">
              <div className="ctp-preview-doc">
                <div className="preview-header">
                  <div className="preview-header-top">
                    <span className="preview-codigo">{metadata.codigo_proceso}</span>
                    <span className="preview-version">V{metadata.version_num}</span>
                  </div>
                  <h1 className="preview-doc-title">{metadata.nombre_documento || 'Acta de Cierre del Proyecto'}</h1>
                </div>

                {(() => {
                  let sectionNum = 0;
                  return blocks.filter((b) => b.activo !== false).map((block) => {
                    if (block.type === 'section') {
                      sectionNum++;
                      return (
                        <div key={block.id} className="preview-section">
                          <h2>{sectionNum}. {block.titulo}</h2>
                        </div>
                      );
                    }
                    if (block.type === 'field') {
                      return (
                        <div key={block.id} className="preview-field">
                          <label>{block.label}</label>
                          <div className="preview-text-inline">&nbsp;</div>
                        </div>
                      );
                    }
                    if (block.type === 'table') {
                      const cols = (block.columnas || []).filter((c) => c.activo !== false);
                      return (
                        <table key={block.id} className="preview-data-table">
                          <thead>
                            <tr>{cols.map((col) => <th key={col.id}>{col.label}</th>)}</tr>
                          </thead>
                          <tbody>
                            <tr>{cols.map((col) => <td key={col.id}>&nbsp;</td>)}</tr>
                            <tr>{cols.map((col) => <td key={col.id}>&nbsp;</td>)}</tr>
                          </tbody>
                        </table>
                      );
                    }
                    if (block.type === 'textarea') {
                      return (
                        <div key={block.id} className="preview-field">
                          <label>{block.label}</label>
                          <div className="preview-text-block">&nbsp;</div>
                        </div>
                      );
                    }
                    if (block.type === 'signature') {
                      return null;
                    }
                    return null;
                  });
                })()}

                {blocks.filter((b) => b.activo !== false).length === 0 && (
                  <p className="preview-empty">No hay bloques activos para previsualizar.</p>
                )}

                <div className="preview-footer">
                  <div className="preview-signatures">
                    {blocks.filter((b) => b.type === 'signature' && b.activo !== false).map((block) => (
                      <div key={block.id} className="preview-sig-block">
                        <div className="preview-sig-line" />
                        <span>{block.label}</span>
                      </div>
                    ))}
                    {blocks.filter((b) => b.type === 'signature' && b.activo !== false).length === 0 && (
                      <>
                        <div className="preview-sig-block">
                          <div className="preview-sig-line" />
                          <span>Director del Proyecto</span>
                        </div>
                        <div className="preview-sig-block">
                          <div className="preview-sig-line" />
                          <span>Patrocinador</span>
                        </div>
                        <div className="preview-sig-block">
                          <div className="preview-sig-line" />
                          <span>Gestor TIC</span>
                        </div>
                      </>
                    )}
                  </div>
                  <p className="preview-footer-text">Documento generado por PROYECTA</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ClosureTemplatePanel;
