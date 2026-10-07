package com.proyecta.api_gestion.service.config;

import com.proyecta.api_gestion.dto.config.ListaParametricaItemDTO;
import com.proyecta.api_gestion.domain.model.config.ListaParametricaConfig;
import com.proyecta.api_gestion.application.port.out.persistence.config.ListaParametricaConfigRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ListaParametricaService {

    private static final Pattern COMILLAS_DOBLES_INICIO = Pattern.compile("^\"+");
    private static final Pattern COMILLAS_DOBLES_FIN = Pattern.compile("\"+$");
    private static final Pattern COMILLAS_SINGLES_INICIO = Pattern.compile("^'+");
    private static final Pattern COMILLAS_SINGLES_FIN = Pattern.compile("'+$");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private final ListaParametricaConfigRepositoryPort repository;

    public ListaParametricaService(ListaParametricaConfigRepositoryPort repository) {
        this.repository = repository;
    }

    private static String limpiarValor(String valor) {
        if (valor == null) {
            return "";
        }
        String nombre = valor.trim();
        nombre = COMILLAS_DOBLES_INICIO.matcher(nombre).replaceAll("");
        nombre = COMILLAS_DOBLES_FIN.matcher(nombre).replaceAll("");
        nombre = COMILLAS_SINGLES_INICIO.matcher(nombre).replaceAll("");
        return COMILLAS_SINGLES_FIN.matcher(nombre).replaceAll("");
    }

    private static String normalizarCodigo(String nombre) {
        return ESPACIOS.matcher(nombre.toUpperCase()).replaceAll("_");
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
            String nombre = limpiarValor(valores.get(i));
            if (nombre.isEmpty()) continue;

            String codigo = normalizarCodigo(nombre);
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
                .map(ListaParametricaService::limpiarValor)
                .filter(v -> !v.isEmpty())
                .map(v -> {
                    String c = normalizarCodigo(v);
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
