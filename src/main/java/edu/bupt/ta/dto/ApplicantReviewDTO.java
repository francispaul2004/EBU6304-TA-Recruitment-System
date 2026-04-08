package edu.bupt.ta.dto;

import java.util.List;

public record ApplicantReviewDTO(
        String applicationId,
        String applicantId,
        String applicantName,
        List<String> technicalSkills,
        List<String> availability,
        int currentHours,
        int projectedHours,
        int maxWeeklyHours,
        String riskLevel,
        String statement,
        String decisionNote,
        int matchScore,
        List<String> missingSkills,
        List<String> matchedSkills,
        String matchExplanation,
        String decisionNote
) {

    public ApplicantReviewDTO(
            String applicationId,
            String applicantId,
            String applicantName,
            List<String> technicalSkills,
            List<String> availability,
            int currentHours,
            int projectedHours,
            int maxWeeklyHours,
            String riskLevel,
            String statement,
            int matchScore,
            List<String> missingSkills
    ) {
        this(applicationId, applicantId, applicantName, technicalSkills, availability,
                currentHours, projectedHours, maxWeeklyHours, riskLevel, statement, matchScore, missingSkills,
                List.of(), "", "");
    }
}
