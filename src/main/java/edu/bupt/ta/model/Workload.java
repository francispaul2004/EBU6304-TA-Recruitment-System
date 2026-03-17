package edu.bupt.ta.model;

import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.repository.Identifiable;

import java.util.ArrayList;
import java.util.List;

public class Workload implements Identifiable<String> {
    private String applicantId;
    private List<String> acceptedJobIds = new ArrayList<>();
    private int currentWeeklyHours;
    private RiskLevel riskLevel;

    public Workload() {
    }

    public Workload(String applicantId, List<String> acceptedJobIds, int currentWeeklyHours, RiskLevel riskLevel) {
        this.applicantId = applicantId;
        this.acceptedJobIds = acceptedJobIds;
        this.currentWeeklyHours = currentWeeklyHours;
        this.riskLevel = riskLevel;
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

    public List<String> getAcceptedJobIds() {
        return acceptedJobIds;
    }

    public void setAcceptedJobIds(List<String> acceptedJobIds) {
        this.acceptedJobIds = acceptedJobIds;
    }

    public int getCurrentWeeklyHours() {
        return currentWeeklyHours;
    }

    public void setCurrentWeeklyHours(int currentWeeklyHours) {
        this.currentWeeklyHours = currentWeeklyHours;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }
}
