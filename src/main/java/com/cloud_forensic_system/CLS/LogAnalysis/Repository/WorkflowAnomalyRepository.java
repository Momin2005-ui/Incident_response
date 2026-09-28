package com.cloud_forensic_system.CLS.LogAnalysis.Repository;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.WorkflowAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowAnomalyRepository extends JpaRepository<WorkflowAnomaly, Long> {
    List<WorkflowAnomaly> findAllByOrderByStartTimeDesc();
}
