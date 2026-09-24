package com.cloud_forensic_system.CLS.LogAnalysis.Repository;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {
    List<LogEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<LogEvent> findByRequestId(String requestId);
}
