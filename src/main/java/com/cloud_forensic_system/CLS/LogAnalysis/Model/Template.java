package com.cloud_forensic_system.CLS.LogAnalysis.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String pattern;
    
    private Integer occurrenceCount = 0;
    private LocalDateTime firstSeen;
}
