package com.proyecta.api_gestion.service.config;

import com.proyecta.api_gestion.dto.config.CatalogOptionDTO;
import com.proyecta.api_gestion.dto.config.PetiCatalogDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.model.config.EstrategiaPetiConfig;
import com.proyecta.api_gestion.repository.config.EstrategiaPetiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class PetiCatalogService {

    private static final List<String> DEFAULT_VIGENCIAS = List.of("2020-2024", "2024-2027", "2027-2030");
    private static final String DEFAULT_ESTRATEGIAS =
            "TECNOLOGIAS_INFORMACION:Tecnologias de la Informacion|" +
            "TRANSFORMACION_DIGITAL:Transformacion Digital|" +
            "CIUDADES_TERRITORIOS_INTELIGENTES:Ciudades y Territorios Inteligentes|" +
            "GOBIERNO_DIGITAL:Gobierno Digital";

    private final SystemParameterService systemParameterService;
    private final EstrategiaPetiConfigRepository estrategiaRepository;

    public PetiCatalogService(SystemParameterService systemParameterService,
                              EstrategiaPetiConfigRepository estrategiaRepository) {
        this.systemParameterService = systemParameterService;
        this.estrategiaRepository = estrategiaRepository;
    }

    @Transactional(readOnly = true)
    public PetiCatalogDTO getCatalog() {
        return new PetiCatalogDTO(getVigencias(), getEstrategias());
    }

    @Transactional(readOnly = true)
    public List<String> getVigencias() {
        return systemParameterService.getCsv(SystemParameterKeys.PETI_VIGENCIAS, DEFAULT_VIGENCIAS);
    }

    @Transactional(readOnly = true)
    public List<CatalogOptionDTO> getEstrategias() {
        String configured = systemParameterService.getString(SystemParameterKeys.PETI_ESTRATEGIAS, DEFAULT_ESTRATEGIAS);
        List<CatalogOptionDTO> fromParameter = parseStrategies(configured);
        if (!fromParameter.isEmpty()) {
            return fromParameter;
        }

        List<CatalogOptionDTO> fromTable = estrategiaRepository.findByActivoTrueOrderByOrdenAsc().stream()
                .map(item -> new CatalogOptionDTO(item.getCodigo(), item.getNombre()))
                .toList();

        return fromTable.isEmpty() ? parseStrategies(DEFAULT_ESTRATEGIAS) : fromTable;
    }

    @Transactional
    public EstrategiaPetiConfig resolveEstrategiaConfig(String value) {
        String code = normalizeCode(value);
        if (code == null) {
            return null;
        }

        CatalogOptionDTO configuredOption = findConfiguredStrategy(code);
        if (configuredOption == null) {
            throw new BadRequestException("La estrategia PETI no esta configurada: " + code);
        }

        EstrategiaPetiConfig existing = estrategiaRepository.findByCodigo(code).orElse(null);
        if (existing != null) {
            boolean changed = false;
            if (!configuredOption.label().equals(existing.getNombre())) {
                existing.setNombre(configuredOption.label());
                changed = true;
            }
            if (!Boolean.TRUE.equals(existing.getActivo())) {
                existing.setActivo(true);
                changed = true;
            }
            return changed ? estrategiaRepository.save(existing) : existing;
        }

        EstrategiaPetiConfig created = new EstrategiaPetiConfig();
        created.setCodigo(code);
        created.setNombre(configuredOption.label());
        created.setDescripcion("Estrategia PETI configurada desde parametros del sistema");
        created.setOrden((int) estrategiaRepository.count() + 1);
        created.setActivo(true);
        return estrategiaRepository.save(created);
    }

    private CatalogOptionDTO findConfiguredStrategy(String code) {
        return getEstrategias().stream()
                .filter(option -> code.equals(normalizeCode(option.value())))
                .findFirst()
                .orElse(null);
    }

    private List<CatalogOptionDTO> parseStrategies(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split("[|,\\r\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(this::parseStrategy)
                .filter(option -> option.value() != null && !option.value().isBlank())
                .distinct()
                .toList();
    }

    private CatalogOptionDTO parseStrategy(String raw) {
        String[] parts = raw.split("[:=]", 2);
        String label = parts.length > 1 ? trimToNull(parts[1]) : trimToNull(raw);
        String code = parts.length > 1 ? normalizeCode(parts[0]) : normalizeCode(label);
        return new CatalogOptionDTO(code, label != null ? label : code);
    }

    public String normalizeCode(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String code = withoutAccents.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return code.isBlank() ? null : code;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
