package com.proyecta.api_gestion.service.notification;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class DefaultNotificationTemplateRulesProvider implements NotificationTemplateRulesProvider {

    private static final Map<String, TemplateRules> RULES = Map.ofEntries(
            Map.entry("PROJECT_INITIAL_REGISTERED", new TemplateRules("PROJECT_INITIAL_REGISTERED", Set.of("projectId", "projectName", "actorUsername", "state", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_INITIAL_COMPLETED", new TemplateRules("PROJECT_INITIAL_COMPLETED", Set.of("projectId", "projectName", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_UPDATED", new TemplateRules("PROJECT_UPDATED", Set.of("projectId", "projectName", "actorUsername", "state", "action", "phaseName", "hitoName", "deliverableName", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_DOCUMENT_UPLOADED", new TemplateRules("PROJECT_DOCUMENT_UPLOADED", Set.of("projectId", "projectName", "documentType", "documentName", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "documentType"))),
            Map.entry("PROJECT_DOCUMENT_DELETED", new TemplateRules("PROJECT_DOCUMENT_DELETED", Set.of("projectId", "projectName", "documentType", "documentName", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "documentType"))),
            Map.entry("PROJECT_DELAYED", new TemplateRules("PROJECT_DELAYED", Set.of("projectId", "projectName", "overdueDeliverables", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "overdueDeliverables"))),
            Map.entry("PROJECT_CLOSED", new TemplateRules("PROJECT_CLOSED", Set.of("projectId", "projectName", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("PROJECT_ASSIGNMENT_CREATED", new TemplateRules("PROJECT_ASSIGNMENT_CREATED", Set.of("projectId", "projectName", "assignmentRole", "assignedUsername", "recipientName"), Set.of("projectId", "assignedUsername"))),
            Map.entry("DELIVERABLE_EVIDENCE_UPLOADED", new TemplateRules("DELIVERABLE_EVIDENCE_UPLOADED", Set.of("projectId", "projectName", "deliverableName", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "deliverableName"))),
            Map.entry("DELIVERABLE_APPROVED", new TemplateRules("DELIVERABLE_APPROVED", Set.of("projectId", "projectName", "deliverableName", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "deliverableName"))),
            Map.entry("DELIVERABLE_REJECTED", new TemplateRules("DELIVERABLE_REJECTED", Set.of("projectId", "projectName", "deliverableName", "observation", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "deliverableName", "observation"))),
            Map.entry("OBSERVATION_SUBSANATED", new TemplateRules("OBSERVATION_SUBSANATED", Set.of("projectId", "projectName", "observationId", "deliverableName", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "observationId"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_REQUIRED", new TemplateRules("PROJECT_BENEFIT_IMPACT_REQUIRED", Set.of("projectId", "projectName", "actorUsername", "benefitImpactUrl", "state", "recipientName"), Set.of("projectId", "projectName", "benefitImpactUrl"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_SUBMITTED", new TemplateRules("PROJECT_BENEFIT_IMPACT_SUBMITTED", Set.of("projectId", "projectName", "actorUsername", "reviewUrl", "state", "recipientName"), Set.of("projectId", "projectName", "reviewUrl"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_RESUBMITTED", new TemplateRules("PROJECT_BENEFIT_IMPACT_RESUBMITTED", Set.of("projectId", "projectName", "actorUsername", "reviewUrl", "state", "recipientName", "previousState"), Set.of("projectId", "projectName", "reviewUrl"))),
            Map.entry("PROJECT_BENEFIT_IMPACT_REVIEWED", new TemplateRules("PROJECT_BENEFIT_IMPACT_REVIEWED", Set.of("projectId", "projectName", "actorUsername", "aprobado", "observaciones", "recipientName"), Set.of("projectId", "projectName", "aprobado"))),
            Map.entry("RISK_CREATED", new TemplateRules("RISK_CREATED", Set.of("projectId", "projectName", "riskCode", "riskLevel", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "riskCode"))),
            Map.entry("RISK_UPDATED", new TemplateRules("RISK_UPDATED", Set.of("projectId", "projectName", "riskCode", "riskLevel", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "riskCode"))),
            Map.entry("RISK_TREATED", new TemplateRules("RISK_TREATED", Set.of("projectId", "projectName", "riskCode", "riskLevel", "actorUsername", "recipientName"), Set.of("projectId", "projectName", "riskCode"))),
            Map.entry("SECURITY_ROLE_UPDATED", new TemplateRules("SECURITY_ROLE_UPDATED", Set.of("roleCode", "actorUsername", "recipientName"), Set.of("roleCode"))),
            Map.entry("CLOSURE_REQUESTED", new TemplateRules("CLOSURE_REQUESTED", Set.of("projectId", "projectName", "requester", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("CLOSURE_APPROVED", new TemplateRules("CLOSURE_APPROVED", Set.of("projectId", "projectName", "approver", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("CLOSURE_REJECTED", new TemplateRules("CLOSURE_REJECTED", Set.of("projectId", "projectName", "rejector", "observaciones", "actorUsername", "recipientName"), Set.of("projectId", "projectName"))),
            Map.entry("ENTREGABLE_FECHA_CAMBIADA", new TemplateRules("ENTREGABLE_FECHA_CAMBIADA", Set.of("projectId", "entregableId", "entregableNombre", "fechaAnterior", "fechaNueva", "justificacion", "recipientName"), Set.of("projectId", "entregableNombre", "fechaNueva"))),
            Map.entry("ENTREGABLE_DEADLINE_WARNING", new TemplateRules("ENTREGABLE_DEADLINE_WARNING", Set.of("projectId", "projectName", "entregableNombre", "entregableId", "diasRestantes", "fechaLimite", "recipientName"), Set.of("projectId", "projectName", "entregableNombre"))),
            Map.entry("ENTREGABLE_OVERDUE_REMINDER", new TemplateRules("ENTREGABLE_OVERDUE_REMINDER", Set.of("projectId", "projectName", "entregableNombre", "entregableId", "diasVencido", "fechaLimite", "recipientName"), Set.of("projectId", "projectName", "entregableNombre")))
    );

    @Override
    public TemplateRules getRules(String eventCode) {
        return RULES.getOrDefault(eventCode, new TemplateRules(eventCode, Set.of("projectId", "projectName", "actorUsername", "recipientName"), Set.of("projectId")));
    }
}
