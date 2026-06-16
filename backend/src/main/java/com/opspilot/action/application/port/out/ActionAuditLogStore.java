package com.opspilot.action.application.port.out;

import com.opspilot.action.domain.ActionAuditLog;
import java.util.List;

public interface ActionAuditLogStore {

    ActionAuditLog save(ActionAuditLog auditLog);

    List<ActionAuditLog> find(String clusterId, String namespace, String targetName);
}
