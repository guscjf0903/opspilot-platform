package com.opspilot.action.adapter.out.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaActionAuditLogRepository extends JpaRepository<ActionAuditLogEntity, UUID> {

    @Query("""
            select log
            from ActionAuditLogEntity log
            where log.clusterId = :clusterId
              and (:namespace is null or log.namespace = :namespace)
              and (:targetName is null or log.targetName = :targetName)
            order by log.createdAt desc
            """)
    List<ActionAuditLogEntity> findLogs(
            @Param("clusterId") String clusterId,
            @Param("namespace") String namespace,
            @Param("targetName") String targetName
    );
}
