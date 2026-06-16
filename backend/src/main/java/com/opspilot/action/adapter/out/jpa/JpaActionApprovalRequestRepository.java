package com.opspilot.action.adapter.out.jpa;

import com.opspilot.action.domain.ActionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaActionApprovalRequestRepository extends JpaRepository<ActionApprovalRequestEntity, UUID> {

    List<ActionApprovalRequestEntity> findByClusterIdAndStatusOrderByRequestedAtDesc(
            String clusterId,
            ActionStatus status
    );

    List<ActionApprovalRequestEntity> findByClusterIdOrderByRequestedAtDesc(String clusterId);
}
