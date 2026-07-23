package com.proyecta.api_gestion.model.config;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lista_parametrica_config", schema = "proyecta_db",
       uniqueConstraints = @UniqueConstraint(columnNames = {"lista_clave", "item_codigo"}))
public class ListaParametricaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "lista_clave", nullable = false, length = 60)
    private String listaClave;

    @Column(name = "item_codigo", nullable = false, length = 200)
    private String itemCodigo;

    @Column(name = "item_nombre", nullable = false, length = 200)
    private String itemNombre;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "lista_nombre_campo", length = 200)
    private String listaNombreCampo;

    @Column(name = "lista_descripcion", length = 500)
    private String listaDescripcion;

    @Column(name = "lista_tipo", length = 30)
    private String listaTipo = "Lista";

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public ListaParametricaConfig() {}

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getListaClave() { return listaClave; }
    public void setListaClave(String listaClave) { this.listaClave = listaClave; }

    public String getItemCodigo() { return itemCodigo; }
    public void setItemCodigo(String itemCodigo) { this.itemCodigo = itemCodigo; }

    public String getItemNombre() { return itemNombre; }
    public void setItemNombre(String itemNombre) { this.itemNombre = itemNombre; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getListaNombreCampo() { return listaNombreCampo; }
    public void setListaNombreCampo(String listaNombreCampo) { this.listaNombreCampo = listaNombreCampo; }

    public String getListaDescripcion() { return listaDescripcion; }
    public void setListaDescripcion(String listaDescripcion) { this.listaDescripcion = listaDescripcion; }

    public String getListaTipo() { return listaTipo; }
    public void setListaTipo(String listaTipo) { this.listaTipo = listaTipo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
