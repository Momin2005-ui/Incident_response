package com.cloud_forensic_system.CLS.LogAnalysis.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class LogEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String level;
    private String service;
    private String requestId;
    
    private Long templateId;
    
    @ElementCollection
    private List<String> parameters;

    @Column(columnDefinition = "TEXT")
    private String rawLineRef;
}
