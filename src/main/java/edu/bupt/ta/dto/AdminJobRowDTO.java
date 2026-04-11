package edu.bupt.ta.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminJobRowDTO(String jobId,
                             String title,
                             String moduleCode,
                             String moduleName,
                             String semester,
                             String organiserId,
                             String organiserName,
                             String typeLabel,
                             String statusLabel,
                             int weeklyHours,
                             int positions,
                             LocalDateTime deadline,
                             LocalDateTime createdAt,
                             int applicantCount,
                             List<String> requiredSkills,
                             List<String> preferredSkills,
                             String description) {
}
