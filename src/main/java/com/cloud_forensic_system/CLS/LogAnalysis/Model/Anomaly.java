package com.cloud_forensic_system.CLS.LogAnalysis.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Anomaly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    
    private String detector;
    private Double score;
    private String status; // e.g. "FLAGGED"
    
    @Column(columnDefinition = "TEXT")
    private String details; // JSON of ranked templates or diagnosis
}
