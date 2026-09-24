package com.cloud_forensic_system.CLS.LogAnalysis.Service;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.Template;
import com.cloud_forensic_system.CLS.LogAnalysis.Repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrainParser {
    
    private final TemplateRepository templateRepository;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    public ParseResult parse(String message) {
        // Preprocess: mask obvious variables like numbers
        String preprocessed = message.replaceAll("\\b\\d+\\b", "<*>");
        String[] tokens = preprocessed.split("\\s+");
        
        List<Template> templates = templateRepository.findAll();
        Template bestMatch = null;
        double maxSimilarity = -1;
        
        for (Template template : templates) {
            String[] templateTokens = template.getPattern().split("\\s+");
            if (templateTokens.length != tokens.length) continue;
            
            double similarity = calculateSimilarity(tokens, templateTokens);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = template;
            }
        }
        
        if (maxSimilarity >= SIMILARITY_THRESHOLD && bestMatch != null) {
            // Update pattern if needed (merge with <*>)
            String newPattern = mergePatterns(tokens, bestMatch.getPattern().split("\\s+"));
            if (!newPattern.equals(bestMatch.getPattern())) {
                bestMatch.setPattern(newPattern);
            }
            bestMatch.setOccurrenceCount(bestMatch.getOccurrenceCount() + 1);
            templateRepository.save(bestMatch);
        } else {
            bestMatch = new Template();
            bestMatch.setPattern(String.join(" ", tokens));
            bestMatch.setOccurrenceCount(1);
            bestMatch.setFirstSeen(LocalDateTime.now());
            bestMatch = templateRepository.save(bestMatch);
        }
        
        List<String> parameters = extractParameters(message.split("\\s+"), bestMatch.getPattern().split("\\s+"));
        return new ParseResult(bestMatch.getId(), parameters);
    }
    
    private double calculateSimilarity(String[] tokens, String[] templateTokens) {
        int matches = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(templateTokens[i]) || templateTokens[i].equals("<*>")) {
                matches++;
            }
        }
        return (double) matches / tokens.length;
    }
    
    private String mergePatterns(String[] tokens, String[] templateTokens) {
        String[] result = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(templateTokens[i])) {
                result[i] = tokens[i];
            } else {
                result[i] = "<*>";
            }
        }
        return String.join(" ", result);
    }
    
    private List<String> extractParameters(String[] rawTokens, String[] templateTokens) {
        List<String> params = new ArrayList<>();
        // if length matches
        if (rawTokens.length == templateTokens.length) {
            for (int i = 0; i < rawTokens.length; i++) {
                if (templateTokens[i].equals("<*>")) {
                    params.add(rawTokens[i]);
                }
            }
        }
        return params;
    }

    public static class ParseResult {
        public Long templateId;
        public List<String> parameters;
        
        public ParseResult(Long templateId, List<String> parameters) {
            this.templateId = templateId;
            this.parameters = parameters;
        }
    }
}
