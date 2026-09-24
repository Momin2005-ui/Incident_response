package com.cloud_forensic_system.CLS.LogAnalysis.Service;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.LogEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HeaderExtractor {
    // Pattern: 2026-09-24 04:15:35.080 INFO  [12345] c.c.CLS.ClsApplication - Starting...
    private static final Pattern LOG_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+\\[(.*?)\\]\\s+(.*?)\\s+-\\s+(.*)$");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LogEvent extract(String rawLine) {
        Matcher matcher = LOG_PATTERN.matcher(rawLine);
        if (matcher.find()) {
            LogEvent event = new LogEvent();
            try {
                event.setTimestamp(LocalDateTime.parse(matcher.group(1), formatter));
            } catch (DateTimeParseException e) {
                event.setTimestamp(LocalDateTime.now());
            }
            event.setLevel(matcher.group(2));
            event.setRequestId(matcher.group(3));
            event.setService(matcher.group(4));
            
            // Temporary store message in rawLineRef to be processed by parser
            event.setRawLineRef(matcher.group(5)); 
            return event;
        }
        return null;
    }
}
