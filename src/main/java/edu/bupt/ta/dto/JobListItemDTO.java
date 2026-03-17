package edu.bupt.ta.dto;

public record JobListItemDTO(String jobId, String title, String moduleCode, String statusLabel,
                             int weeklyHours, String deadlineLabel) {
}
