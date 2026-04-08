package edu.bupt.ta.dto;

public record AdminWorkloadRowDTO(String applicantId,
                                  String applicantName,
                                  String studentId,
                                  int acceptedJobs,
                                  int currentWeeklyHours,
                                  int maxWeeklyHours,
                                  String riskLevel,
                                  String note) {
}
