package edu.bupt.ta.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminApplicationRowDTO(String applicationId,
                                     String jobId,
                                     String jobTitle,
                                     String applicantId,
                                     String applicantName,
                                     String studentId,
                                     String organiserId,
                                     String organiserName,
                                     String statusLabel,
                                     LocalDateTime applyDate,
                                     int matchScore,
                                     int currentWeeklyHours,
                                     int projectedWeeklyHours,
                                     String riskLevel,
                                     String decisionNote,
                                     String statement,
                                     List<String> missingSkills) {
}
