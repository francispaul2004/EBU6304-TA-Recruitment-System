package edu.bupt.ta.model;

import edu.bupt.ta.repository.Identifiable;

import java.time.LocalDateTime;

public class ApplicantProfile implements Identifiable<String> {
    private String applicantId;
    private String userId;
    private String fullName;
    private String studentId;
    private String programme;
    private int year;
    private String email;
    private String phone;
    private LocalDateTime lastUpdated;

    public ApplicantProfile() {
    }

    public ApplicantProfile(String applicantId, String userId, String fullName, String studentId, String programme,
                            int year, String email, String phone, LocalDateTime lastUpdated) {
        this.applicantId = applicantId;
        this.userId = userId;
        this.fullName = fullName;
        this.studentId = studentId;
        this.programme = programme;
        this.year = year;
        this.email = email;
        this.phone = phone;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
