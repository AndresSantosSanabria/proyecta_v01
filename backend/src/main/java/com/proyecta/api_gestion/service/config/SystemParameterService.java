package com.proyecta.api_gestion.service.config;

import com.proyecta.api_gestion.repository.SystemParameterRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SystemParameterService {

    private final SystemParameterRepository systemParameterRepository;

    public SystemParameterService(SystemParameterRepository systemParameterRepository) {
        this.systemParameterRepository = systemParameterRepository;
    }

    public Optional<String> getString(String key) {
        return systemParameterRepository.findByKey(normalizeKey(key)).map(param -> trimToNull(param.getValue()));
    }

    public String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return getString(key)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return getString(key)
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public List<String> getCsv(String key, List<String> defaultValues) {
        return getString(key)
                .map(value -> Arrays.stream(value.split(","))
                        .map(this::trimToNull)
                        .filter(item -> item != null && !item.isBlank())
                        .toList())
                .filter(values -> !values.isEmpty())
                .orElse(defaultValues);
    }

    private String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
