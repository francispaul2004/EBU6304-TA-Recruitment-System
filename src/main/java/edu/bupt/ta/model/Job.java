package edu.bupt.ta.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.repository.Identifiable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Job implements Identifiable<String> {
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String jobId;
    private String title;
    private String moduleCode;
    private String moduleName;
    private String semester;
    private JobType type;
    private String description;
    private List<String> requiredSkills = new ArrayList<>();
    private List<String> preferredSkills = new ArrayList<>();
    private int weeklyHours;
    private int positions;
    private LocalDateTime deadline;
    private String organiserId;
    private JobStatus status;
    private LocalDateTime createdAt;

    public Job() {
    }

    public Job(String jobId, String title, String moduleCode, String moduleName, String semester, JobType type, String description,
               List<String> requiredSkills, List<String> preferredSkills, int weeklyHours, int positions,
               LocalDateTime deadline, String organiserId, JobStatus status, LocalDateTime createdAt) {
        this.jobId = jobId;
        this.title = title;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.semester = semester;
        this.type = type;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.preferredSkills = preferredSkills;
        this.weeklyHours = weeklyHours;
        this.positions = positions;
        this.deadline = deadline;
        this.organiserId = organiserId;
        this.status = status;
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public List<String> getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(List<String> preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    public int getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(int weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public int getPositions() {
        return positions;
    }

    public void setPositions(int positions) {
        this.positions = positions;
    }

    @JsonIgnore
    public LocalDateTime getDeadline() {
        return deadline;
    }

    @JsonIgnore
    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    @JsonGetter("deadline")
    public String getDeadlineJson() {
        return deadline == null ? null : deadline.format(DEADLINE_FORMAT);
    }

    @JsonSetter("deadline")
    public void setDeadlineJson(String value) {
        if (value == null || value.isBlank()) {
            this.deadline = null;
            return;
        }
        String normalized = value.trim();
        this.deadline = normalized.contains("T")
                ? LocalDateTime.parse(normalized, DEADLINE_FORMAT)
                : LocalDateTime.parse(normalized + "T23:59:00", DEADLINE_FORMAT);
    }

    public String getOrganiserId() {
        return organiserId;
    }

    public void setOrganiserId(String organiserId) {
        this.organiserId = organiserId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
