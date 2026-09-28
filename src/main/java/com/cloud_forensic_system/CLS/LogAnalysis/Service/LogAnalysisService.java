package com.cloud_forensic_system.CLS.LogAnalysis.Service;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.Anomaly;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.LogEvent;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.Template;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.AnomalyRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.LogEventRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.cloud_forensic_system.CLS.LogAnalysis.Repository.WorkflowAnomalyRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LogAnalysisService {
    
    private final HeaderExtractor headerExtractor;
    private final DrainParser drainParser;
    private final LogEventRepository logEventRepository;
    private final TemplateRepository templateRepository;
    private final AnomalyRepository anomalyRepository;
    private final WorkflowAnalyzer workflowAnalyzer;
    private final WorkflowAnomalyRepository workflowAnomalyRepository;

    public String analyzeLogFile(String filePath) {
        try {
            // Clear all previous analysis data so every run is a fresh, clean result
            workflowAnomalyRepository.deleteAll();
            anomalyRepository.deleteAll();
            logEventRepository.deleteAll();
            templateRepository.deleteAll();
            
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LogEvent event = headerExtractor.extract(line);
                    if (event != null) {
                        DrainParser.ParseResult result = drainParser.parse(event.getRawLineRef());
                        event.setTemplateId(result.templateId);
                        event.setParameters(result.parameters);
                        event.setRawLineRef(null);
                        logEventRepository.save(event);
                        count++;
                    }
                }
            } catch (Exception e) {
                return "Error reading file: " + e.getMessage();
            }
            
            // Tier 1: Z-Score on event-count vectors (Infrastructure anomalies)
            detectAnomalies();
            
            // Tier 2: Workflow sequence analysis per MDC requestId (Business-logic anomalies)
            workflowAnalyzer.analyze();
            
            return "Processed " + count + " log lines successfully.";
        } catch (Exception e) {
            return "Database Error: Ensure your PostgreSQL container is running. (" + e.getMessage() + ")";
        }
    }
    
    private void detectAnomalies() {
        List<LogEvent> allEvents = logEventRepository.findAll();
        if (allEvents.isEmpty()) return;
        
        // Sort by timestamp
        allEvents.sort(Comparator.comparing(LogEvent::getTimestamp));
        
        LocalDateTime start = allEvents.get(0).getTimestamp().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = allEvents.get(allEvents.size() - 1).getTimestamp().truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        
        // Group by 60s windows
        Map<LocalDateTime, Map<Long, Integer>> windowCounts = new HashMap<>();
        for (LogEvent event : allEvents) {
            LocalDateTime window = event.getTimestamp().truncatedTo(ChronoUnit.MINUTES);
            windowCounts.putIfAbsent(window, new HashMap<>());
            windowCounts.get(window).put(event.getTemplateId(), windowCounts.get(window).getOrDefault(event.getTemplateId(), 0) + 1);
        }
        
        // Sort windows to simulate online detection
        List<LocalDateTime> sortedWindows = new ArrayList<>(windowCounts.keySet());
        Collections.sort(sortedWindows);
        
        List<Template> templates = templateRepository.findAll();
        
        for (int i = 0; i < sortedWindows.size(); i++) {
            LocalDateTime window = sortedWindows.get(i);
            Map<Long, Integer> counts = windowCounts.get(window);
            
            boolean isAnomaly = false;
            double maxScore = 0;
            List<String> detailsList = new ArrayList<>();
            
            for (Template t : templates) {
                long tid = t.getId();
                int currentCount = counts.getOrDefault(tid, 0);
                if (currentCount == 0) continue; // Skip if it didn't happen in this window
                
                // Calculate baseline from PREVIOUS windows only
                double sum = 0;
                int baselineWindows = i; 
                
                if (baselineWindows > 0) {
                    for (int j = 0; j < i; j++) {
                        sum += windowCounts.get(sortedWindows.get(j)).getOrDefault(tid, 0);
                    }
                    double m = sum / baselineWindows;
                    
                    double variance = 0;
                    for (int j = 0; j < i; j++) {
                        double val = windowCounts.get(sortedWindows.get(j)).getOrDefault(tid, 0);
                        variance += Math.pow(val - m, 2);
                    }
                    double sd = Math.sqrt(variance / baselineWindows);
                    
                    if (sd > 0) {
                        double z = Math.abs(currentCount - m) / sd;
                        if (z > 2.0) { // Using 2.0 for demonstration purposes
                            isAnomaly = true;
                            maxScore = Math.max(maxScore, z);
                            detailsList.add(String.format("Template %d (Z=%.2f, count=%d, avg=%.2f)", tid, z, currentCount, m));
                        }
                    } else if (currentCount > m) {
                        // Standard deviation is 0 (constant baseline), but current is higher
                        isAnomaly = true;
                        maxScore = Math.max(maxScore, 5.0);
                        detailsList.add(String.format("Template %d (Spike from baseline %.1f to %d)", tid, m, currentCount));
                    }
                } else {
                    // First window ever, everything is new, don't flag as anomaly unless we want to.
                    // For demo, we assume the first window is normal startup.
                }
            }
            
            if (isAnomaly) {
                Anomaly anomaly = new Anomaly();
                anomaly.setWindowStart(window);
                anomaly.setWindowEnd(window.plusMinutes(1));
                anomaly.setDetector("Z-Score / Baseline Spike");
                anomaly.setScore(maxScore);
                anomaly.setStatus("FLAGGED");
                anomaly.setDetails(String.join("; ", detailsList));
                anomalyRepository.save(anomaly);
            }
        }
    }
}
