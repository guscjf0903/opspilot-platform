package com.opspilot.action.adapter.out.jpa;

import com.opspilot.action.application.port.out.ActionApprovalStore;
import com.opspilot.action.domain.ActionApprovalRequest;
import com.opspilot.action.domain.ActionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaActionApprovalStore implements ActionApprovalStore {

    private final JpaActionApprovalRequestRepository repository;

    @Override
    public ActionApprovalRequest save(ActionApprovalRequest approvalRequest) {
        ActionApprovalRequestEntity entity = repository.findById(approvalRequest.id())
                .orElseGet(() -> ActionApprovalRequestEntity.fromDomain(approvalRequest));
        entity.update(approvalRequest);

        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<ActionApprovalRequest> findById(UUID id) {
        return repository.findById(id).map(ActionApprovalRequestEntity::toDomain);
    }

    @Override
    public List<ActionApprovalRequest> find(String clusterId, ActionStatus status) {
        if (status == null) {
            return repository.findByClusterIdOrderByRequestedAtDesc(clusterId)
                    .stream()
                    .map(ActionApprovalRequestEntity::toDomain)
                    .toList();
        }

        return repository.findByClusterIdAndStatusOrderByRequestedAtDesc(clusterId, status)
                .stream()
                .map(ActionApprovalRequestEntity::toDomain)
                .toList();
    }
}
