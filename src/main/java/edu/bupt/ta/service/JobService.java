package edu.bupt.ta.service;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.AuditLogEntry;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.repository.AuditLogRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.IdGenerator;
import edu.bupt.ta.util.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobService {

    private final JobRepository jobRepository;
    private final AuditLogRepository auditLogRepository;

    public JobService(JobRepository jobRepository, AuditLogRepository auditLogRepository) {
        this.jobRepository = jobRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public ValidationResult createJob(Job job) {
        ValidationResult validation = validate(job);
        if (!validation.isValid()) {
            return validation;
        }

        if (job.getJobId() == null || job.getJobId().isBlank()) {
            String nextId = IdGenerator.next("J", jobRepository.findAll().stream().map(Job::getJobId).toList(), 3);
            job.setJobId(nextId);
        }
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.DRAFT);
        }
        if (job.getCreatedAt() == null) {
            job.setCreatedAt(DateTimeUtils.now());
        }

        jobRepository.save(job);
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), job.getOrganiserId(), "CREATE_JOB",
                job.getJobId() + " created with status " + job.getStatus()));
        return ValidationResult.ok();
    }

    public ValidationResult updateJob(Job job) {
        ValidationResult validation = validate(job);
        if (!validation.isValid()) {
            return validation;
        }

        Optional<Job> existing = jobRepository.findById(job.getJobId());
        if (existing.isEmpty()) {
            return ValidationResult.fail("Job not found.");
        }
        if (!existing.get().getOrganiserId().equals(job.getOrganiserId())) {
            return ValidationResult.fail("You can only edit your own jobs.");
        }

        jobRepository.save(job);
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), job.getOrganiserId(), "UPDATE_JOB",
                job.getJobId() + " updated"));
        return ValidationResult.ok();
    }

    public void closeJob(String jobId, String organiserId) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return;
        }
        Job job = jobOpt.get();
        if (!job.getOrganiserId().equals(organiserId)) {
            return;
        }

        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), organiserId, "CLOSE_JOB",
                jobId + " set to CLOSED"));
    }

    public List<Job> searchJobs(JobSearchCriteria criteria) {
        refreshExpiredStatuses();
        return jobRepository.search(criteria);
    }

    public Optional<Job> getJob(String jobId) {
        refreshExpiredStatuses();
        return jobRepository.findById(jobId);
    }

    public List<Job> getJobsByOrganiser(String organiserId) {
        refreshExpiredStatuses();
        return jobRepository.findByOrganiserId(organiserId);
    }

    public void refreshExpiredStatuses() {
        List<Job> jobs = new ArrayList<>(jobRepository.findAll());
        boolean changed = false;
        for (Job job : jobs) {
            if (job.getStatus() == JobStatus.OPEN && job.getDeadline() != null
                    && job.getDeadline().isBefore(DateTimeUtils.today())) {
                job.setStatus(JobStatus.EXPIRED);
                changed = true;
            }
        }
        if (changed) {
            jobRepository.saveAll(jobs);
        }
    }

    private ValidationResult validate(Job job) {
        List<String> errors = new ArrayList<>();
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            errors.add("Title is required.");
        }
        if (job.getModuleCode() == null || job.getModuleCode().isBlank()) {
            errors.add("Module code is required.");
        }
        if (job.getModuleName() == null || job.getModuleName().isBlank()) {
            errors.add("Module name is required.");
        }
        if (job.getWeeklyHours() <= 0) {
            errors.add("Weekly hours must be greater than 0.");
        }
        if (job.getPositions() <= 0) {
            errors.add("Positions must be greater than 0.");
        }
        if (job.getDeadline() == null) {
            errors.add("Deadline is required.");
        }
        if (job.getOrganiserId() == null || job.getOrganiserId().isBlank()) {
            errors.add("Organiser ID is required.");
        }
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }
}
