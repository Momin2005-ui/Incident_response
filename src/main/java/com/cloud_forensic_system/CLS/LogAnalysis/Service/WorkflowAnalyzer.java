package com.cloud_forensic_system.CLS.LogAnalysis.Service;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.LogEvent;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.Template;
import com.cloud_forensic_system.CLS.LogAnalysis.Model.WorkflowAnomaly;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.LogEventRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.TemplateRepository;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.WorkflowAnomalyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowAnalyzer {

    private final LogEventRepository logEventRepository;
    private final TemplateRepository templateRepository;
    private final WorkflowAnomalyRepository workflowAnomalyRepository;

    public void analyze() {
        workflowAnomalyRepository.deleteAll();

        // --- Separation: business events have a non-empty requestId (set by MDC)
        //     infrastructure logs (Tomcat, Hibernate, etc.) have requestId = ""
        List<LogEvent> businessEvents = logEventRepository.findAll()
                .stream()
                .filter(e -> e.getRequestId() != null && !e.getRequestId().isBlank())
                .collect(Collectors.toList());

        if (businessEvents.isEmpty()) return;

        // Build a map of templateId → pattern for readable output
        Map<Long, String> templatePatterns = templateRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Template::getId, Template::getPattern));

        // Group events by requestId, sort each group by timestamp
        Map<String, List<LogEvent>> byRequest = businessEvents.stream()
                .collect(Collectors.groupingBy(LogEvent::getRequestId));

        byRequest.forEach((requestId, events) ->
                events.sort(Comparator.comparing(LogEvent::getTimestamp)));

        // ── STEP 1: Learn normal sequence lengths ──────────────────────────────
        // A sequence is "normal" if it has no ERROR-level events.
        // We group normal sequences by their first templateId (workflow type proxy)
        // and compute median length.
        Map<Long, List<Integer>> normalLengthsByType = new HashMap<>();
        for (Map.Entry<String, List<LogEvent>> entry : byRequest.entrySet()) {
            List<LogEvent> seq = entry.getValue();
            boolean hasError = seq.stream().anyMatch(e -> "ERROR".equals(e.getLevel()));
            if (!hasError && !seq.isEmpty()) {
                Long firstTemplate = seq.get(0).getTemplateId();
                normalLengthsByType
                        .computeIfAbsent(firstTemplate, k -> new ArrayList<>())
                        .add(seq.size());
            }
        }
        // compute median per workflow type
        Map<Long, Double> medianLength = new HashMap<>();
        normalLengthsByType.forEach((type, lengths) -> {
            Collections.sort(lengths);
            double median = lengths.size() % 2 == 0
                    ? (lengths.get(lengths.size() / 2 - 1) + lengths.get(lengths.size() / 2)) / 2.0
                    : lengths.get(lengths.size() / 2);
            medianLength.put(type, median);
        });

        // ── STEP 2: Detect anomalies per request ──────────────────────────────
        for (Map.Entry<String, List<LogEvent>> entry : byRequest.entrySet()) {
            String requestId = entry.getKey();
            List<LogEvent> seq = entry.getValue();
            if (seq.isEmpty()) continue;

            long errorCount = seq.stream().filter(e -> "ERROR".equals(e.getLevel())).count();
            long warnCount  = seq.stream().filter(e -> "WARN".equals(e.getLevel())).count();
            double errorRatio = (double) errorCount / seq.size();

            Long firstTemplate = seq.get(0).getTemplateId();
            double expectedLen = medianLength.getOrDefault(firstTemplate, 1.0);
            double lengthDeviation = expectedLen > 0 ? (double) seq.size() / expectedLen : 1.0;

            String anomalyType = null;
            double score = 0;
            List<String> details = new ArrayList<>();

            // Rule 1 – ERROR_ESCALATION: sequence starts with INFO/WARN then hits ERROR
            // Detects: retry → pool exhausted → checkout timeout  (the case-study scenario)
            boolean startsNormal = "INFO".equals(seq.get(0).getLevel()) || "WARN".equals(seq.get(0).getLevel());
            boolean endsWithError = "ERROR".equals(seq.get(seq.size() - 1).getLevel());
            if (startsNormal && endsWithError && errorCount > 0) {
                anomalyType = "ERROR_ESCALATION";
                score = Math.max(score, 5.0 + errorCount);
                seq.stream()
                   .filter(e -> "ERROR".equals(e.getLevel()) || "WARN".equals(e.getLevel()))
                   .forEach(e -> details.add("[" + e.getLevel() + "] "
                           + templatePatterns.getOrDefault(e.getTemplateId(), "Template " + e.getTemplateId())));
            }

            // Rule 2 – HIGH_ERROR_RATIO: more than 30% of steps are ERRORs
            if (errorRatio > 0.30) {
                anomalyType = anomalyType == null ? "HIGH_ERROR_RATIO" : anomalyType;
                score = Math.max(score, errorRatio * 10);
                details.add(String.format("%.0f%% of steps are ERROR level (%d/%d)",
                        errorRatio * 100, errorCount, seq.size()));
            }

            // Rule 3 – LONG_SEQUENCE: sequence is > 2× expected length (retries bloating it)
            if (lengthDeviation > 2.0 && (warnCount + errorCount) > 0) {
                anomalyType = anomalyType == null ? "LONG_SEQUENCE" : anomalyType;
                score = Math.max(score, lengthDeviation);
                details.add(String.format("Sequence length %d is %.1fx the expected %.0f (possible retry loop)",
                        seq.size(), lengthDeviation, expectedLen));
            }

            if (anomalyType != null) {
                WorkflowAnomaly wa = new WorkflowAnomaly();
                wa.setRequestId(requestId);
                wa.setStartTime(seq.get(0).getTimestamp());
                wa.setEndTime(seq.get(seq.size() - 1).getTimestamp());
                wa.setAnomalyType(anomalyType);
                wa.setDeviationScore(score);
                wa.setDetails(String.join(" → ", details));
                workflowAnomalyRepository.save(wa);
            }
        }
    }
}
