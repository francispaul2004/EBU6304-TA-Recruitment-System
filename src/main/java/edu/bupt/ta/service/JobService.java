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
        return createJob(job, job.getOrganiserId());
    }

    public ValidationResult createJob(Job job, String actorUserId) {
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
        String actor = resolveActorUserId(actorUserId, job.getOrganiserId());
        String detail = job.getJobId() + " created with status " + job.getStatus();
        if (!actor.equals(job.getOrganiserId())) {
            detail += " for organiser " + job.getOrganiserId();
        }
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), actor, "CREATE_JOB", detail));
        return ValidationResult.ok();
    }

    public ValidationResult updateJob(Job job) {
        return updateJob(job, job.getOrganiserId(), false);
    }

    public ValidationResult updateJob(Job job, String actorUserId, boolean adminOverride) {
        if (job == null || job.getJobId() == null || job.getJobId().isBlank()) {
            return ValidationResult.fail("Job not found.");
        }

        Optional<Job> existing = jobRepository.findById(job.getJobId());
        if (existing.isEmpty()) {
            return ValidationResult.fail("Job not found.");
        }

        Job existingJob = existing.get();
        if (!adminOverride && !existingJob.getOrganiserId().equals(actorUserId)) {
            return ValidationResult.fail("You can only edit your own jobs.");
        }

        if (!adminOverride) {
            job.setOrganiserId(existingJob.getOrganiserId());
        }
        if (job.getCreatedAt() == null) {
            job.setCreatedAt(existingJob.getCreatedAt());
        }
        if (job.getSemester() == null || job.getSemester().isBlank()) {
            job.setSemester(existingJob.getSemester());
        }

        ValidationResult validation = validate(job);
        if (!validation.isValid()) {
            return validation;
        }

        jobRepository.save(job);
        String actor = resolveActorUserId(actorUserId, job.getOrganiserId());
        String detail = job.getJobId() + " updated";
        if (adminOverride && !existingJob.getOrganiserId().equals(job.getOrganiserId())) {
            detail += " and reassigned to organiser " + job.getOrganiserId();
        }
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), actor, "UPDATE_JOB", detail));
        return ValidationResult.ok();
    }

    public void closeJob(String jobId, String organiserId) {
        closeJobWithValidation(jobId, organiserId);
    }

    public ValidationResult closeJobWithValidation(String jobId, String organiserId) {
        return closeJobWithValidation(jobId, organiserId, false);
    }

    public ValidationResult closeJobWithValidation(String jobId, String actorUserId, boolean adminOverride) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ValidationResult.fail("Job not found.");
        }
        Job job = jobOpt.get();
        if (!adminOverride && !job.getOrganiserId().equals(actorUserId)) {
            return ValidationResult.fail("You can only close your own jobs.");
        }
        if (job.getStatus() != JobStatus.OPEN) {
            return ValidationResult.fail("Only OPEN jobs can be closed.");
        }

        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(),
                resolveActorUserId(actorUserId, job.getOrganiserId()), "CLOSE_JOB",
                jobId + " set to CLOSED"));
        return ValidationResult.ok();
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

    private String resolveActorUserId(String actorUserId, String fallbackUserId) {
        if (actorUserId != null && !actorUserId.isBlank()) {
            return actorUserId;
        }
        return fallbackUserId == null || fallbackUserId.isBlank() ? "SYSTEM" : fallbackUserId;
    }
}
