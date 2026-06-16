package com.opspilot.ai.adapter.out.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaIncidentAnalysisReportRepository extends JpaRepository<IncidentAnalysisReportEntity, UUID> {

    List<IncidentAnalysisReportEntity> findByClusterIdOrderByCreatedAtDesc(String clusterId);
}
