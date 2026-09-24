package com.cloud_forensic_system.CLS.LogAnalysis.Repository;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
    List<Anomaly> findAllByOrderByWindowStartDesc();
}
