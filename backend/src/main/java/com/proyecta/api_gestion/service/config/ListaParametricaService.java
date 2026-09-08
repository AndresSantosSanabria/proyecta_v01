package com.proyecta.api_gestion.service.config;

import com.proyecta.api_gestion.dto.config.ListaParametricaItemDTO;
import com.proyecta.api_gestion.dto.config.ListaParametricaUpsertRequest;
import com.proyecta.api_gestion.model.config.ListaParametricaConfig;
import com.proyecta.api_gestion.repository.config.ListaParametricaConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListaParametricaService {

    private final ListaParametricaConfigRepository repository;

    public ListaParametricaService(ListaParametricaConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ListaParametricaItemDTO> listarPorClave(String listaClave, boolean soloActivos) {
        List<ListaParametricaConfig> items = soloActivos
                ? repository.findByListaClaveAndActivoTrueOrderByOrdenAsc(listaClave)
                : repository.findByListaClaveOrderByOrdenAsc(listaClave);
        return items.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarValoresActivos(String listaClave) {
        return repository.findByListaClaveAndActivoTrueOrderByOrdenAsc(listaClave)
                .stream()
                .map(ListaParametricaConfig::getItemNombre)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListaParametricaItemDTO> listarMetadatas() {
        List<ListaParametricaConfig> all = repository.findAll();
        return all.stream()
                .collect(java.util.stream.Collectors.groupingBy(ListaParametricaConfig::getListaClave))
                .entrySet().stream()
                .map(e -> {
                    ListaParametricaConfig first = e.getValue().get(0);
                    return new ListaParametricaItemDTO(
                            null,
                            e.getKey(),
                            null,
                            null,
                            null,
                            null,
                            first.getListaNombreCampo(),
                            first.getListaDescripcion(),
                            first.getListaTipo()
                    );
                })
                .toList();
    }

    @Transactional
    public ListaParametricaItemDTO guardar(ListaParametricaUpsertRequest request) {
        ListaParametricaConfig entity = repository
                .findByListaClaveAndItemCodigo(request.listaClave(), request.itemCodigo())
                .orElseGet(ListaParametricaConfig::new);

        entity.setListaClave(request.listaClave());
        entity.setItemCodigo(request.itemCodigo());
        entity.setItemNombre(request.itemNombre());
        entity.setOrden(request.orden() != null ? request.orden() : 0);
        entity.setActivo(request.activo() != null ? request.activo() : true);

        if (Boolean.TRUE.equals(request.saveMetadata())) {
            entity.setListaNombreCampo(request.listaNombreCampo());
            entity.setListaDescripcion(request.listaDescripcion());
            entity.setListaTipo(request.listaTipo() != null ? request.listaTipo() : "Lista");
        }

        return toDTO(repository.save(entity));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void toggleActivo(Long id) {
        ListaParametricaConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item no encontrado con id: " + id));
        entity.setActivo(!entity.getActivo());
        repository.save(entity);
    }

    @Transactional
    public void guardarValores(String listaClave, String nombreCampo, String descripcion, List<String> valores) {
        List<ListaParametricaConfig> existentes = repository.findByListaClaveOrderByOrdenAsc(listaClave);

        for (int i = 0; i < valores.size(); i++) {
            String nombre = valores.get(i).trim().replaceAll("^\"+|\"+$", "").replaceAll("^'+|'+$", "");
            if (nombre.isEmpty()) continue;

            String codigo = nombre.toUpperCase().replaceAll("\\s+", "_");
            if (codigo.length() > 190) {
                codigo = codigo.substring(0, 190);
            }
            final String finalCodigo = codigo;
            ListaParametricaConfig entity = existentes.stream()
                    .filter(e -> e.getItemCodigo().equals(finalCodigo))
                    .findFirst()
                    .orElseGet(ListaParametricaConfig::new);

            entity.setListaClave(listaClave);
            entity.setItemCodigo(finalCodigo);
            entity.setItemNombre(nombre);
            entity.setOrden(i + 1);
            entity.setActivo(true);
            entity.setListaNombreCampo(nombreCampo);
            entity.setListaDescripcion(descripcion);
            entity.setListaTipo("Lista");

            repository.save(entity);
        }

        java.util.Set<String> nuevosCodigos = valores.stream()
                .map(v -> v.trim().replaceAll("^\"+|\"+$", "").replaceAll("^'+|'+$", ""))
                .filter(v -> !v.isEmpty())
                .map(v -> {
                    String c = v.toUpperCase().replaceAll("\\s+", "_");
                    return c.length() > 190 ? c.substring(0, 190) : c;
                })
                .collect(java.util.stream.Collectors.toSet());

        for (ListaParametricaConfig existente : existentes) {
            if (!nuevosCodigos.contains(existente.getItemCodigo())) {
                repository.delete(existente);
            }
        }
    }

    private ListaParametricaItemDTO toDTO(ListaParametricaConfig entity) {
        return new ListaParametricaItemDTO(
                entity.getId(),
                entity.getListaClave(),
                entity.getItemCodigo(),
                entity.getItemNombre(),
                entity.getOrden(),
                entity.getActivo(),
                entity.getListaNombreCampo(),
                entity.getListaDescripcion(),
                entity.getListaTipo()
        );
    }
}
