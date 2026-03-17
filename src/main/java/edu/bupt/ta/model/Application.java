package edu.bupt.ta.model;

import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.repository.Identifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Application implements Identifiable<String> {
    private String applicationId;
    private String jobId;
    private String applicantId;
    private LocalDateTime applyDate;
    private String statement;
    private ApplicationStatus status;
    private String decisionNote;
    private int matchScore;
    private List<String> missingSkills = new ArrayList<>();

    public Application() {
    }

    public Application(String applicationId, String jobId, String applicantId, LocalDateTime applyDate,
                       String statement, ApplicationStatus status, String decisionNote,
                       int matchScore, List<String> missingSkills) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.applicantId = applicantId;
        this.applyDate = applyDate;
        this.statement = statement;
        this.status = status;
        this.decisionNote = decisionNote;
        this.matchScore = matchScore;
        this.missingSkills = missingSkills;
    }

    @Override
    public String getId() {
        return applicationId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public LocalDateTime getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(LocalDateTime applyDate) {
        this.applyDate = applyDate;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }
}
