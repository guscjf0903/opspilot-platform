package com.opspilot.action.application.port.out;

import com.opspilot.action.domain.ActionApprovalRequest;
import com.opspilot.action.domain.ActionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionApprovalStore {

    ActionApprovalRequest save(ActionApprovalRequest approvalRequest);

    Optional<ActionApprovalRequest> findById(UUID id);

    List<ActionApprovalRequest> find(String clusterId, ActionStatus status);
}
