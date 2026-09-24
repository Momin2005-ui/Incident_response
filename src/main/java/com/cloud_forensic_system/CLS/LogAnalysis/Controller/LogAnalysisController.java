package com.cloud_forensic_system.CLS.LogAnalysis.Controller;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.Anomaly;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.Template;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.AnomalyRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.TemplateRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogAnalysisController {
    
    private final LogAnalysisService logAnalysisService;
    private final TemplateRepository templateRepository;
    private final AnomalyRepository anomalyRepository;
    private static final Logger logger = LoggerFactory.getLogger(LogAnalysisController.class);

    @PostMapping("/simulate-incident")
    public ResponseEntity<?> simulateIncident() throws InterruptedException {
        // Simulate the case study scenario (Section 5.2 of PDF)
        String reqId = UUID.randomUUID().toString();
        MDC.put("requestId", reqId);
        
        // 02:00 Normal traffic, then payment gateway slows down
        logger.info("Payment authorized for order 88213 amount 1499.00 INR");
        logger.info("Payment authorized for order 88214 amount 850.00 INR");
        logger.warn("Retrying gateway call, attempt 1 of 3, timeout 1000 ms");
        Thread.sleep(100);
        
        // 02:03 Retries pile up
        logger.warn("Retrying gateway call, attempt 2 of 3, timeout 3000 ms");
        logger.warn("Retrying gateway call, attempt 3 of 3, timeout 5000 ms");
        logger.info("Payment authorized for order 88215 amount 200.00 INR");
        Thread.sleep(100);
        
        // 02:04 Connection pool exhausted
        logger.error("Connection pool exhausted: 50/50 in use");
        logger.error("Connection pool exhausted: 50/50 in use");
        Thread.sleep(100);
        
        // 02:06 Checkout waits on payment and starts timing out
        logger.error("Checkout timeout after 10000ms waiting for payment service");
        logger.error("Checkout timeout after 10000ms waiting for payment service");
        
        MDC.remove("requestId");
        
        return ResponseEntity.ok(Map.of("message", "Simulated flash sale incident logs written to application.log"));
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeLogs() {
        // Hardcoded path to the application log for the prototype
        String result = logAnalysisService.analyzeLogFile("logs/application.log");
        return ResponseEntity.ok(Map.of("message", result));
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<Template>> getTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }
    
    @GetMapping("/anomalies")
    public ResponseEntity<List<Anomaly>> getAnomalies() {
        return ResponseEntity.ok(anomalyRepository.findAllByOrderByWindowStartDesc());
    }
    
    @GetMapping("/stats/compression")
    public ResponseEntity<?> getCompressionStats() {
        // Mock compression stats for now, as exact calculation requires 
        // serializing DB records and comparing to raw file size
        Map<String, Object> stats = new HashMap<>();
        stats.put("algorithm", "Logzip-style Template Storage");
        stats.put("compressionRatio", "8.5x");
        stats.put("originalSize", "2.4 MB");
        stats.put("compressedSize", "282 KB");
        return ResponseEntity.ok(stats);
    }
}
