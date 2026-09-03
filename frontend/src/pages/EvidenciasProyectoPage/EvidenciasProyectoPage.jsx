import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  FileText, Search, Eye, Calendar, User, Tag, ChevronDown, ChevronUp,
  AlertCircle, LoaderCircle, FolderOpen, ClipboardCheck, Clock, Shield, FileEdit,
} from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import { formatDate } from '../../utils/locale';
import EvidenciaDetailModal from '../../components/projects/EvidenciaDetailModal';
import './EvidenciasProyectoPage.css';

const CATEGORIAS = [
  { key: 'TODOS', label: 'Todos', icon: FolderOpen },
  { key: 'DOCUMENTO_PROYECTO', label: 'Documentos del Proyecto', icon: FileText },
  { key: 'EVIDENCIA_ENTREGABLE', label: 'Evidencias de Entregables', icon: ClipboardCheck },
  { key: 'CRONOGRAMA', label: 'Cronograma', icon: FileText },
  { key: 'RIESGO', label: 'Soluciones de Riesgos', icon: Shield },
  { key: 'CAMBIO_FECHA', label: 'Cambios de Fecha', icon: Clock },
  { key: 'CAMBIO_DESCRIPCION', label: 'Cambios de Descripción', icon: FileEdit },
];

const DOCUMENT_LABELS = {
  VIABILIZACION: 'Documento de viabilidad',
  ACTA_CONSTITUCION: 'Acta de constitucion',
  CRONOGRAMA: 'Cronograma del proyecto',
  PLAN_COMUNICACIONES: 'Plan de comunicaciones',
};

const unwrapDocuments = (response) => {
  const payload = response?.data?.data ?? response?.data ?? response;
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.documentos)) return payload.documentos;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
};

const getUserDisplayName = (user) => {
  if (!user) return '';
  if (typeof user === 'string') return user;
  return user?.nombre || user?.name || user?.username || user?.correo || user?.email || '';
};

const mapProjectDocument = (doc, index, projectData) => {
  const tipoDocumento = String(
    doc?.tipoDocumentoCodigo
      || doc?.tipo_documento_codigo
      || doc?.tipoDocumento
      || doc?.tipo_documento
      || ''
  ).toUpperCase();
  const nombreArchivo = doc?.nombreOriginal
    || doc?.nombre_original
    || doc?.nombreAlmacenado
    || doc?.nombre_almacenado
    || '';
  const fechaRegistro = doc?.fechaCarga || doc?.fecha_carga || doc?.fechaRegistro || doc?.fecha_registro || null;
  const usuario = getUserDisplayName(
    doc?.usuario
      || doc?.usuarioCarga
      || doc?.usuario_carga
      || doc?.usuarioNombre
      || doc?.usuario_nombre
      || doc?.nombreUsuario
      || doc?.nombre_usuario
      || doc?.cargadoPor
      || doc?.cargado_por
      || doc?.uploadedBy
      || doc?.createdBy
  ) || getUserDisplayName(projectData?.directorNombre || projectData?.director);

  return {
    id: `documento-${doc?.id || tipoDocumento || index}`,
    categoria: 'DOCUMENTO_PROYECTO',
    nombre: DOCUMENT_LABELS[tipoDocumento] || tipoDocumento || 'Documento del proyecto',
    nombreArchivo,
    evidenciaUrl: null,
    fechaRegistro,
    fechaEntrega: null,
    fechaLimite: null,
    estado: 'CARGADO',
    estadoCodigo: 'CARGADO',
    usuario,
    tipo: tipoDocumento || 'DOCUMENTO_PROYECTO',
    tipoDocumento,
    faseNombre: null,
    hitoNombre: null,
    entregableNombre: null,
    entregableId: null,
    descripcion: doc?.observacion || doc?.observacionCarga || doc?.observacion_carga || '',
    observaciones: doc?.observacion || doc?.observacionCarga || doc?.observacion_carga || '',
    archivoPdf: null,
  };
};

const EvidenciasProyectoPage = () => {
  const params = useParams();
  const codigoProyecto = (params.codigoProyecto || params.id || '').toUpperCase();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [allItems, setAllItems] = useState([]);
  const [projectInfo, setProjectInfo] = useState(null);
  const [searchText, setSearchText] = useState('');
  const [filterCategoria, setFilterCategoria] = useState('TODOS');
  const [sortField, setSortField] = useState('fechaRegistro');
  const [sortDirection, setSortDirection] = useState('desc');
  const [selectedItem, setSelectedItem] = useState(null);

  const fetchAllData = useCallback(async () => {
    if (!codigoProyecto) return;
    try {
      setLoading(true);
      setError(null);

      const [evidenciasResponse, projectResponse, documentosResponse] = await Promise.allSettled([
        projectService.getEvidenciasByProyecto(codigoProyecto),
        projectService.getById(codigoProyecto),
        documentService.listarDocumentos(codigoProyecto),
      ]);

      const evidenciasData = evidenciasResponse.status === 'fulfilled'
        ? (evidenciasResponse.value?.data?.data ?? evidenciasResponse.value?.data ?? [])
        : [];
      const projectData = projectResponse.status === 'fulfilled'
        ? (projectResponse.value?.data?.data ?? projectResponse.value?.data ?? null)
        : null;
      const documentosData = documentosResponse.status === 'fulfilled'
        ? unwrapDocuments(documentosResponse.value)
        : [];

      const items = Array.isArray(evidenciasData) ? evidenciasData.map((ev) => ({
        id: ev.id,
        categoria: ev.categoria,
        nombre: ev.nombre || 'Sin nombre',
        nombreArchivo: ev.nombreArchivo || '',
        evidenciaUrl: ev.evidenciaUrl || null,
        fechaRegistro: ev.fechaRegistro || null,
        fechaEntrega: ev.fechaEntrega || null,
        fechaLimite: ev.fechaLimite || null,
        estado: ev.estado || 'CARGADO',
        estadoCodigo: ev.estadoCodigo || ev.estado || 'CARGADO',
        usuario: ev.usuario || '',
        tipo: ev.tipo || '',
        tipoDocumento: ev.tipoDocumento || null,
        faseNombre: ev.faseNombre || null,
        hitoNombre: ev.hitoNombre || null,
        entregableNombre: ev.entregableNombre || null,
        entregableId: ev.entregableId || null,
        descripcion: ev.descripcion || '',
        observaciones: ev.observaciones || '',
        cambioId: ev.cambioId || null,
        riesgoId: ev.riesgoId || null,
        riesgoSolucionId: ev.riesgoSolucionId || null,
        archivoPdf: ev.archivoPdf || null,
        fechaAnterior: ev.fechaAnterior || null,
        fechaNueva: ev.fechaNueva || null,
        descripcionAnterior: ev.descripcionAnterior || null,
        descripcionNueva: ev.descripcionNueva || null,
        justificacion: ev.justificacion || '',
      })) : [];

      setAllItems([
        ...documentosData.map((doc, index) => mapProjectDocument(doc, index, projectData)),
        ...items,
      ]);
      setProjectInfo(projectData);
    } catch (err) {
      console.error('Error fetching evidencias:', err);
      setError('No se pudieron cargar las evidencias del proyecto.');
    } finally {
      setLoading(false);
    }
  }, [codigoProyecto]);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const SortIcon = ({ field }) => {
    if (sortField !== field) return <ChevronDown size={14} style={{ opacity: 0.3 }} />;
    return sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />;
  };

  const filteredItems = allItems
    .filter((ev) => {
      if (filterCategoria !== 'TODOS' && ev.categoria !== filterCategoria) return false;
      if (searchText) {
        const q = searchText.toLowerCase();
        return (
          (ev.nombre || '').toLowerCase().includes(q) ||
          (ev.entregableNombre || '').toLowerCase().includes(q) ||
          (ev.usuario || '').toLowerCase().includes(q) ||
          (ev.faseNombre || '').toLowerCase().includes(q) ||
          (ev.hitoNombre || '').toLowerCase().includes(q) ||
          (ev.tipo || '').toLowerCase().includes(q) ||
          (ev.descripcion || '').toLowerCase().includes(q)
        );
      }
      return true;
    })
    .sort((a, b) => {
      let valA, valB;
      switch (sortField) {
        case 'nombre':
          valA = (a.nombre || '').toLowerCase();
          valB = (b.nombre || '').toLowerCase();
          break;
        case 'fechaRegistro':
          valA = a.fechaRegistro || '';
          valB = b.fechaRegistro || '';
          break;
        case 'categoria':
          valA = (a.categoria || '').toLowerCase();
          valB = (b.categoria || '').toLowerCase();
          break;
        case 'usuario':
          valA = (a.usuario || '').toLowerCase();
          valB = (b.usuario || '').toLowerCase();
          break;
        default:
          valA = a.fechaRegistro || '';
          valB = b.fechaRegistro || '';
      }
      if (valA < valB) return sortDirection === 'asc' ? -1 : 1;
      if (valA > valB) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

  const getCatCount = (cat) => {
    if (cat === 'TODOS') return allItems.length;
    return allItems.filter((it) => it.categoria === cat).length;
  };

  const getCategoriaBadge = (cat) => {
    const map = {
      DOCUMENTO_PROYECTO: { bg: 'var(--primary-soft)', color: 'var(--primary)', label: 'Documento' },
      EVIDENCIA_ENTREGABLE: { bg: 'var(--success-soft)', color: 'var(--success)', label: 'Evidencia' },
      CRONOGRAMA: { bg: 'var(--info-soft, #e0f2fe)', color: 'var(--info, #0284c7)', label: 'Cronograma' },
      RIESGO: { bg: 'var(--danger-soft)', color: 'var(--danger)', label: 'Riesgo' },
      CAMBIO_FECHA: { bg: 'var(--warning-soft)', color: 'var(--warning)', label: 'Cambio fecha' },
      CAMBIO_DESCRIPCION: { bg: 'var(--primary-soft)', color: 'var(--primary)', label: 'Cambio descripción' },
    };
    return map[cat] || { bg: 'var(--bg-muted)', color: 'var(--text-muted)', label: cat };
  };

  if (loading) {
    return (
      <div className="evp-container">
        <div className="evp-loading">
          <LoaderCircle size={28} className="animate-spin" />
          <span>Cargando documentos y evidencias...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="evp-container">
      <div className="evp-header">
        <div className="evp-header-content">
          <span className="evp-kicker">Evidencias de Gestion</span>
          <h1>{projectInfo?.nombre || 'Proyecto'}</h1>
          <p>{codigoProyecto}{projectInfo?.dependencia ? ` - ${projectInfo.dependencia}` : ''}</p>
        </div>
        <div className="evp-header-stats">
          <div className="evp-stat-card">
            <span className="evp-stat-label">Total</span>
            <strong className="evp-stat-value">{allItems.length}</strong>
          </div>
          <div className="evp-stat-card evp-stat-primary">
            <span className="evp-stat-label">Documentos</span>
            <strong className="evp-stat-value">{getCatCount('DOCUMENTO_PROYECTO')}</strong>
          </div>
          <div className="evp-stat-card evp-stat-success">
            <span className="evp-stat-label">Evidencias</span>
            <strong className="evp-stat-value">{getCatCount('EVIDENCIA_ENTREGABLE')}</strong>
          </div>
          <div className="evp-stat-card evp-stat-warning">
            <span className="evp-stat-label">Cambios Fecha</span>
            <strong className="evp-stat-value">{getCatCount('CAMBIO_FECHA')}</strong>
          </div>
        </div>
      </div>

      {error && (
        <div className="evp-error-banner">
          <AlertCircle size={16} />
          <span>{error}</span>
          <button onClick={fetchAllData}>Reintentar</button>
        </div>
      )}

      <div className="evp-tabs">
        {CATEGORIAS.map((cat) => {
          const Icon = cat.icon;
          const count = getCatCount(cat.key);
          return (
            <button
              key={cat.key}
              type="button"
              className={`evp-tab ${filterCategoria === cat.key ? 'active' : ''}`}
              onClick={() => setFilterCategoria(cat.key)}
            >
              <Icon size={16} />
              <span>{cat.label}</span>
              <span className="evp-tab-count">{count}</span>
            </button>
          );
        })}
      </div>

      <div className="evp-toolbar">
        <div className="evp-search-box">
          <Search size={16} />
          <input
            type="text"
            placeholder="Buscar por nombre, entregable, fase, tipo..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
        </div>
      </div>

      {allItems.length === 0 ? (
        <div className="evp-empty-state">
          <FileText size={48} strokeWidth={1} />
          <h3>No hay documentos ni evidencias</h3>
          <p>Este proyecto aun no tiene documentos o evidencias de gestion asociados.</p>
        </div>
      ) : (
        <div className="evp-table-container">
          <div className="evp-table-wrapper">
            <table className="evp-table">
              <thead>
                <tr>
                  <th className="evp-th-sortable" onClick={() => handleSort('nombre')}>
                    <span>Nombre</span>
                    <SortIcon field="nombre" />
                  </th>
                  <th onClick={() => handleSort('fechaRegistro')}>
                    <span>Fecha</span>
                    <SortIcon field="fechaRegistro" />
                  </th>
                  <th onClick={() => handleSort('categoria')}>
                    <span>Categoria</span>
                    <SortIcon field="categoria" />
                  </th>
                  <th>Tipo</th>
                  <th onClick={() => handleSort('usuario')}>
                    <span>Usuario</span>
                    <SortIcon field="usuario" />
                  </th>
                  <th>Ubicacion</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredItems.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="evp-no-results">
                      No se encontraron elementos con los filtros aplicados.
                    </td>
                  </tr>
                ) : (
                  filteredItems.map((item) => {
                    const catBadge = getCategoriaBadge(item.categoria);
                    return (
                      <tr key={item.id} className="evp-row" onClick={() => setSelectedItem(item)}>
                        <td className="evp-td-name">
                          <div className="evp-td-name-content">
                            {item.categoria === 'DOCUMENTO_PROYECTO' && <FileText size={16} className="evp-td-icon evp-td-icon--primary" />}
                            {item.categoria === 'EVIDENCIA_ENTREGABLE' && <ClipboardCheck size={16} className="evp-td-icon evp-td-icon--success" />}
                            {item.categoria === 'CRONOGRAMA' && <FileText size={16} className="evp-td-icon" style={{ color: 'var(--info, #0284c7)' }} />}
                            {item.categoria === 'RIESGO' && <Shield size={16} className="evp-td-icon" style={{ color: 'var(--danger)' }} />}
                            {item.categoria === 'CAMBIO_FECHA' && <Clock size={16} className="evp-td-icon evp-td-icon--warning" />}
                            {item.categoria === 'CAMBIO_DESCRIPCION' && <FileEdit size={16} className="evp-td-icon evp-td-icon--primary" />}
                            <div>
                              <strong>{item.nombre || 'Sin nombre'}</strong>
                              {item.entregableNombre && <span className="evp-td-subtitle">{item.entregableNombre}</span>}
                              {item.nombreArchivo && item.nombre !== item.nombreArchivo && <span className="evp-td-subtitle">{item.nombreArchivo}</span>}
                            </div>
                          </div>
                        </td>
                        <td>
                          <span className="evp-td-date">
                            <Calendar size={12} />
                            {formatDate(item.fechaRegistro) || 'Sin fecha'}
                          </span>
                        </td>
                        <td>
                          <span
                            className="evp-cat-badge"
                            style={{ background: catBadge.bg, color: catBadge.color }}
                          >
                            {catBadge.label}
                          </span>
                        </td>
                        <td>
                          <span className="evp-td-type">
                            <Tag size={12} />
                            {item.tipo}
                          </span>
                        </td>
                        <td>
                          <span className="evp-td-user">
                            <User size={12} />
                            {item.usuario || 'Sin usuario'}
                          </span>
                        </td>
                        <td className="evp-td-hierarchy">
                          {item.faseNombre && (
                            <>
                              <span>{item.faseNombre}</span>
                              <span className="evp-td-hierarchy-sep">/</span>
                              <span>{item.hitoNombre}</span>
                            </>
                          )}
                          {!item.faseNombre && item.categoria === 'DOCUMENTO_PROYECTO' && (
                            <span>Documento del proyecto</span>
                          )}
                          {!item.faseNombre && item.categoria === 'CRONOGRAMA' && (
                            <span>Cronograma del proyecto</span>
                          )}
                          {!item.faseNombre && item.categoria === 'RIESGO' && (
                            <span>Solucion de riesgo</span>
                          )}
                        </td>
                        <td className="evp-td-actions">
                          <button
                            type="button"
                            className="evp-btn-view"
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedItem(item);
                            }}
                            title="Ver detalle"
                          >
                            <Eye size={16} />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          {filteredItems.length > 0 && (
            <div className="evp-table-footer">
              <span>Mostrando {filteredItems.length} de {allItems.length} elementos</span>
            </div>
          )}
        </div>
      )}

      <EvidenciaDetailModal
        evidencia={selectedItem}
        open={!!selectedItem}
        onClose={() => setSelectedItem(null)}
        proyectoId={codigoProyecto}
      />
    </div>
  );
};

export default EvidenciasProyectoPage;
