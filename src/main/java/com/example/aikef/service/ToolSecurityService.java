package com.example.aikef.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ToolSecurityService {

    // Blacklist of dangerous Python modules/functions
    private static final List<String> BLACKLISTED_KEYWORDS = Arrays.asList(
            "os.system", "subprocess", "eval", "exec", "open(", 
            "shutil", "sys.modules", "__import__", "requests", "urllib",
            "socket", "telnetlib", "pty", "commands"
    );

    // Regex to detect potential SQL injection patterns (basic heuristic)
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(drop\\s+table|delete\\s+from|insert\\s+into|update\\s+\\w+\\s+set|truncate\\s+table|grant\\s+all|revoke\\s+all|union\\s+select)"
    );

    /**
     * Validate the generated Python code for security risks.
     * @param code The Python code to validate.
     * @throws SecurityException if the code contains dangerous patterns.
     */
    public void validateCode(String code) throws SecurityException {
        if (code == null || code.trim().isEmpty()) {
            throw new SecurityException("Code cannot be empty");
        }

        // 1. Keyword Blacklist Check
        for (String keyword : BLACKLISTED_KEYWORDS) {
            if (code.contains(keyword)) {
                log.warn("Security Violation: Code contains blacklisted keyword '{}'", keyword);
                throw new SecurityException("Security Violation: Code contains restricted keyword: " + keyword);
            }
        }

        // 2. SQL Injection Heuristic Check
        // Although Lambda runs in isolation, we still check for obvious malicious SQL patterns 
        // in case the tool connects to a database.
        if (SQL_INJECTION_PATTERN.matcher(code).find()) {
            log.warn("Security Violation: Code contains potential SQL injection pattern");
            throw new SecurityException("Security Violation: Code contains potential malicious SQL patterns");
        }

        // 3. Length Limit (prevent DOS via massive code)
        if (code.length() > 50000) {
            throw new SecurityException("Code length exceeds limit (50KB)");
        }
        
        log.info("Code passed static security validation.");
    }
}
