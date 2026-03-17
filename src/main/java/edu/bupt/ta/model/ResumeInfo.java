package edu.bupt.ta.model;

import edu.bupt.ta.repository.Identifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResumeInfo implements Identifiable<String> {
    private String applicantId;
    private List<String> relevantModules = new ArrayList<>();
    private List<String> technicalSkills = new ArrayList<>();
    private List<String> languageSkills = new ArrayList<>();
    private String experienceText;
    private String personalStatement;
    private List<String> availability = new ArrayList<>();
    private int maxWeeklyHours;
    private LocalDateTime lastUpdated;

    public ResumeInfo() {
    }

    public ResumeInfo(String applicantId, List<String> relevantModules, List<String> technicalSkills,
                      List<String> languageSkills, String experienceText, String personalStatement,
                      List<String> availability, int maxWeeklyHours, LocalDateTime lastUpdated) {
        this.applicantId = applicantId;
        this.relevantModules = relevantModules;
        this.technicalSkills = technicalSkills;
        this.languageSkills = languageSkills;
        this.experienceText = experienceText;
        this.personalStatement = personalStatement;
        this.availability = availability;
        this.maxWeeklyHours = maxWeeklyHours;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String getId() {
        return applicantId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public List<String> getRelevantModules() {
        return relevantModules;
    }

    public void setRelevantModules(List<String> relevantModules) {
        this.relevantModules = relevantModules;
    }

    public List<String> getTechnicalSkills() {
        return technicalSkills;
    }

    public void setTechnicalSkills(List<String> technicalSkills) {
        this.technicalSkills = technicalSkills;
    }

    public List<String> getLanguageSkills() {
        return languageSkills;
    }

    public void setLanguageSkills(List<String> languageSkills) {
        this.languageSkills = languageSkills;
    }

    public String getExperienceText() {
        return experienceText;
    }

    public void setExperienceText(String experienceText) {
        this.experienceText = experienceText;
    }

    public String getPersonalStatement() {
        return personalStatement;
    }

    public void setPersonalStatement(String personalStatement) {
        this.personalStatement = personalStatement;
    }

    public List<String> getAvailability() {
        return availability;
    }

    public void setAvailability(List<String> availability) {
        this.availability = availability;
    }

    public int getMaxWeeklyHours() {
        return maxWeeklyHours;
    }

    public void setMaxWeeklyHours(int maxWeeklyHours) {
        this.maxWeeklyHours = maxWeeklyHours;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
