package com.opspilot.action.adapter.out.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.action.application.port.out.ActionAuditLogStore;
import com.opspilot.action.domain.ActionAuditLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaActionAuditLogStore implements ActionAuditLogStore {

    private final JpaActionAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public ActionAuditLog save(ActionAuditLog auditLog) {
        return repository.save(ActionAuditLogEntity.fromDomain(auditLog, objectMapper))
                .toDomain(objectMapper);
    }

    @Override
    public List<ActionAuditLog> find(String clusterId, String namespace, String targetName) {
        return repository.findLogs(clusterId, normalize(namespace), normalize(targetName))
                .stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
