package edu.bupt.ta.dto;

public record ApplicantListItemDTO(String applicationId, String applicantId, String fullName,
                                   String status, int matchScore, int currentHours) {
}
