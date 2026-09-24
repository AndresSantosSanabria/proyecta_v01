package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_interno")
public class DocumentoInterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", length = 30, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 255, nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "nombre_original", length = 255, nullable = false)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", length = 255, nullable = false, unique = true)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", length = 500, nullable = false)
    private String rutaAlmacenamiento;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "creado_por", length = 200, nullable = false)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public DocumentoInterno() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDate.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getRutaAlmacenamiento() { return rutaAlmacenamiento; }
    public void setRutaAlmacenamiento(String rutaAlmacenamiento) { this.rutaAlmacenamiento = rutaAlmacenamiento; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
