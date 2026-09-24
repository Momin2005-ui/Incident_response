package com.cloud_forensic_system.CLS.LogAnalysis.Service;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.Anomaly;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.LogEvent;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.Template;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.AnomalyRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.LogEventRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public String analyzeLogFile(String filePath) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEvent event = headerExtractor.extract(line);
                if (event != null) {
                    DrainParser.ParseResult result = drainParser.parse(event.getRawLineRef());
                    event.setTemplateId(result.templateId);
                    event.setParameters(result.parameters);
                    // Store as compressed (just template ID and params, we can drop rawLineRef)
                    event.setRawLineRef(null);
                    logEventRepository.save(event);
                    count++;
                }
            }
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
        
        // After processing, run anomaly detection
        detectAnomalies();
        
        return "Processed " + count + " log lines successfully.";
    }
    
    private void detectAnomalies() {
        anomalyRepository.deleteAll(); // clear old runs for prototype
        
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
        
        // Simple baseline: mean and std dev of counts per template across all windows
        Map<Long, Double> mean = new HashMap<>();
        Map<Long, Double> stdDev = new HashMap<>();
        List<Template> templates = templateRepository.findAll();
        
        int numWindows = windowCounts.size();
        for (Template t : templates) {
            long tid = t.getId();
            double sum = 0;
            for (Map<Long, Integer> counts : windowCounts.values()) {
                sum += counts.getOrDefault(tid, 0);
            }
            double m = sum / numWindows;
            mean.put(tid, m);
            
            double variance = 0;
            for (Map<Long, Integer> counts : windowCounts.values()) {
                double val = counts.getOrDefault(tid, 0);
                variance += Math.pow(val - m, 2);
            }
            stdDev.put(tid, Math.sqrt(variance / numWindows));
        }
        
        // Detect anomalies using Z-score > 3
        for (Map.Entry<LocalDateTime, Map<Long, Integer>> entry : windowCounts.entrySet()) {
            LocalDateTime window = entry.getKey();
            Map<Long, Integer> counts = entry.getValue();
            
            boolean isAnomaly = false;
            double maxScore = 0;
            List<String> detailsList = new ArrayList<>();
            
            for (Template t : templates) {
                long tid = t.getId();
                int count = counts.getOrDefault(tid, 0);
                double m = mean.get(tid);
                double sd = stdDev.get(tid);
                
                if (sd > 0) {
                    double z = Math.abs(count - m) / sd;
                    if (z > 3.0) {
                        isAnomaly = true;
                        maxScore = Math.max(maxScore, z);
                        detailsList.add(String.format("Template %d (Z=%.2f, count=%d, avg=%.2f)", tid, z, count, m));
                    }
                } else if (count > 0 && m == 0) {
                    // New template seen only in this window
                    isAnomaly = true;
                    maxScore = Math.max(maxScore, 5.0);
                    detailsList.add(String.format("Template %d (New, count=%d)", tid, count));
                }
            }
            
            if (isAnomaly) {
                Anomaly anomaly = new Anomaly();
                anomaly.setWindowStart(window);
                anomaly.setWindowEnd(window.plusMinutes(1));
                anomaly.setDetector("Z-Score / New Template");
                anomaly.setScore(maxScore);
                anomaly.setStatus("FLAGGED");
                anomaly.setDetails(String.join("; ", detailsList));
                anomalyRepository.save(anomaly);
            }
        }
    }
}
