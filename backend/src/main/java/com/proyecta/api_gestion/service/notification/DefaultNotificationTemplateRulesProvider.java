package com.proyecta.api_gestion.service.notification;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class DefaultNotificationTemplateRulesProvider implements NotificationTemplateRulesProvider {

    private static final Map<String, TemplateRules> RULES = Map.ofEntries(
            Map.entry("PROJECT_INITIAL_REGISTERED", new TemplateRules("PROJECT_INITIAL_REGISTERED", Set.of("projectId", "projectName", "actorUsername", "state", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_INITIAL_COMPLETED", new TemplateRules("PROJECT_INITIAL_COMPLETED", Set.of("projectId", "projectName", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_UPDATED", new TemplateRules("PROJECT_UPDATED", Set.of("projectId", "projectName", "actorUsername", "state", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_DELAYED", new TemplateRules("PROJECT_DELAYED", Set.of("projectId", "projectName", "overdueDeliverables", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "overdueDeliverables"))),
            Map.entry("PROJECT_CLOSED", new TemplateRules("PROJECT_CLOSED", Set.of("projectId", "projectName", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_ASSIGNMENT_CREATED", new TemplateRules("PROJECT_ASSIGNMENT_CREATED", Set.of("projectId", "assignmentRole", "assignedUsername", "recipientName"), Set.of("projectId", "assignedUsername"))),
            Map.entry("DELIVERABLE_EVIDENCE_UPLOADED", new TemplateRules("DELIVERABLE_EVIDENCE_UPLOADED", Set.of("projectId", "projectName", "deliverableName", "actorUsername", "recipientName"), Set.of("projectId", "deliverableName"))),
            Map.entry("DELIVERABLE_APPROVED", new TemplateRules("DELIVERABLE_APPROVED", Set.of("projectId", "projectName", "deliverableName", "actorUsername", "recipientName"), Set.of("projectId", "deliverableName"))),
            Map.entry("DELIVERABLE_REJECTED", new TemplateRules("DELIVERABLE_REJECTED", Set.of("projectId", "projectName", "deliverableName", "observation", "actorUsername", "recipientName"), Set.of("projectId", "deliverableName", "observation"))),
            Map.entry("OBSERVATION_SUBSANATED", new TemplateRules("OBSERVATION_SUBSANATED", Set.of("projectId", "observationId", "deliverableName", "actorUsername", "recipientName"), Set.of("projectId", "observationId"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_REQUIRED", new TemplateRules("PROJECT_BENEFIT_IMPACT_REQUIRED", Set.of("projectId", "projectName", "actorUsername", "benefitImpactUrl", "state", "recipientName"), Set.of("projectId", "projectName", "benefitImpactUrl"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_SUBMITTED", new TemplateRules("PROJECT_BENEFIT_IMPACT_SUBMITTED", Set.of("projectId", "projectName", "actorUsername", "reviewUrl", "state", "recipientName"), Set.of("projectId", "projectName", "reviewUrl"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_REVIEWED", new TemplateRules("PROJECT_BENEFIT_IMPACT_REVIEWED", Set.of("projectId", "projectName", "actorUsername", "aprobado", "observaciones", "recipientName"), Set.of("projectId", "projectName", "aprobado"))),
            Map.entry("RISK_CREATED", new TemplateRules("RISK_CREATED", Set.of("projectId", "riskCode", "riskLevel", "actorUsername", "recipientName"), Set.of("projectId", "riskCode"))),
            Map.entry("RISK_UPDATED", new TemplateRules("RISK_UPDATED", Set.of("projectId", "riskCode", "riskLevel", "actorUsername", "recipientName"), Set.of("projectId", "riskCode"))),
            Map.entry("RISK_TREATED", new TemplateRules("RISK_TREATED", Set.of("projectId", "riskCode", "riskLevel", "actorUsername", "recipientName"), Set.of("projectId", "riskCode"))),
            Map.entry("SECURITY_USER_UPDATED", new TemplateRules("SECURITY_USER_UPDATED", Set.of("username", "actorUsername", "recipientName"), Set.of("username"))),
            Map.entry("SECURITY_ROLE_UPDATED", new TemplateRules("SECURITY_ROLE_UPDATED", Set.of("roleCode", "actorUsername", "recipientName"), Set.of("roleCode")))
    );

    @Override
    public TemplateRules getRules(String eventCode) {
        return RULES.getOrDefault(eventCode, new TemplateRules(eventCode, Set.of("projectId", "projectName", "actorUsername", "recipientName"), Set.of("projectId")));
    }
}
