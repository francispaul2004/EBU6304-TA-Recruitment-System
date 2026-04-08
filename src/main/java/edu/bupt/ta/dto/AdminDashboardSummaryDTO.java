package edu.bupt.ta.dto;

public record AdminDashboardSummaryDTO(long totalJobs,
                                       long totalApplications,
                                       long acceptedApplications,
                                       long highRiskApplicants) {
}
