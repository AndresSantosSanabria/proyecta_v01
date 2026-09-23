package com.proyecta.api_gestion.service.config;

import com.proyecta.api_gestion.dto.config.ListaParametricaItemDTO;
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
