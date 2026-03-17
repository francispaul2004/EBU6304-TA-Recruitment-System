package edu.bupt.ta.dto;

public record DashboardSummaryDTO(int profileCompletion,
                                  int resumeCompletion,
                                  int currentWorkload,
                                  int applicationCount) {
}
