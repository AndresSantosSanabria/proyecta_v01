package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegexTemplateVariableValidator implements TemplateVariableValidator {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");
    private final NotificationTemplateRulesProvider rulesProvider;

    public RegexTemplateVariableValidator(NotificationTemplateRulesProvider rulesProvider) {
        this.rulesProvider = rulesProvider;
    }

    @Override
    public void validate(String eventCode, String subjectTemplate, String bodyTemplate) {
        TemplateRules rules = rulesProvider.getRules(eventCode);
        Set<String> used = new LinkedHashSet<>();
        used.addAll(extract(subjectTemplate));
        used.addAll(extract(bodyTemplate));

        Set<String> missing = new HashSet<>(rules.requiredVariables());
        missing.removeAll(used);

        Set<String> prohibited = new HashSet<>(used);
        prohibited.removeAll(rules.allowedVariables());

        if (!missing.isEmpty()) {
            throw new BadRequestException("Faltan variables obligatorias: " + String.join(", ", missing));
        }

        if (!prohibited.isEmpty()) {
            throw new BadRequestException("Variables no permitidas en la plantilla: " + String.join(", ", prohibited));
        }
    }

    private Set<String> extract(String template) {
        if (template == null || template.isBlank()) {
            return Set.of();
        }
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        Set<String> tokens = new LinkedHashSet<>();
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        return tokens;
    }
}
