package edu.bupt.ta.dto;

import edu.bupt.ta.enums.RiskLevel;

public record WorkloadSummaryDTO(String applicantId, int currentHours, int maxWeeklyHours,
                                 int projectedHours, RiskLevel riskLevel) {
}
