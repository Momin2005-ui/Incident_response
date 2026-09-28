package com.cloud_forensic_system.CLS.LogAnalysis.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class WorkflowAnomaly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // "ERROR_ESCALATION" | "HIGH_ERROR_RATIO" | "LONG_SEQUENCE"
    private String anomalyType;

    private Double deviationScore;

    @Column(columnDefinition = "TEXT")
    private String details; // human-readable root cause
}
