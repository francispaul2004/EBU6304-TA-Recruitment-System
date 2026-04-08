package edu.bupt.ta.service;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.dto.WorkloadSummaryDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.AuditLogEntry;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.AuditLogRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.ValidationResult;

import java.util.List;
import java.util.Optional;

public class ReviewService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ResumeInfoRepository resumeInfoRepository;
    private final WorkloadService workloadService;
    private final AuditLogRepository auditLogRepository;

    public ReviewService(ApplicationRepository applicationRepository,
                         JobRepository jobRepository,
                         ApplicantProfileRepository profileRepository,
                         ResumeInfoRepository resumeInfoRepository,
                         WorkloadService workloadService,
                         AuditLogRepository auditLogRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.resumeInfoRepository = resumeInfoRepository;
        this.workloadService = workloadService;
        this.auditLogRepository = auditLogRepository;
    }

    public ValidationResult acceptApplication(String applicationId, String organiserId, String decisionNote) {
        return acceptApplication(applicationId, organiserId, decisionNote, false);
    }

    public ValidationResult acceptApplication(String applicationId,
                                              String actorUserId,
                                              String decisionNote,
                                              boolean adminOverride) {
        Optional<Application> appOpt = applicationRepository.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ValidationResult.fail("Application not found.");
        }
        Application application = appOpt.get();

        Optional<Job> jobOpt = jobRepository.findById(application.getJobId());
        if (jobOpt.isEmpty()) {
            return ValidationResult.fail("Job not found.");
        }

        Job job = jobOpt.get();
        if (!hasReviewPermission(job, actorUserId, adminOverride)) {
            return ValidationResult.fail("Permission denied: not your job.");
        }

        application.setStatus(ApplicationStatus.ACCEPTED);
        application.setDecisionNote(decisionNote == null ? "" : decisionNote.trim());
        applicationRepository.save(application);

        workloadService.refreshWorkloadForApplicant(application.getApplicantId());
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), actorUserId,
                "ACCEPT_APPLICATION", applicationId + " accepted"));
        return ValidationResult.ok();
    }

    public ValidationResult rejectApplication(String applicationId, String organiserId, String decisionNote) {
        return rejectApplication(applicationId, organiserId, decisionNote, false);
    }

    public ValidationResult rejectApplication(String applicationId,
                                              String actorUserId,
                                              String decisionNote,
                                              boolean adminOverride) {
        Optional<Application> appOpt = applicationRepository.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ValidationResult.fail("Application not found.");
        }
        Application application = appOpt.get();

        Optional<Job> jobOpt = jobRepository.findById(application.getJobId());
        if (jobOpt.isEmpty()) {
            return ValidationResult.fail("Job not found.");
        }

        Job job = jobOpt.get();
        if (!hasReviewPermission(job, actorUserId, adminOverride)) {
            return ValidationResult.fail("Permission denied: not your job.");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setDecisionNote(decisionNote == null ? "" : decisionNote.trim());
        applicationRepository.save(application);

        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), actorUserId,
                "REJECT_APPLICATION", applicationId + " rejected"));
        return ValidationResult.ok();
    }

    public ApplicantReviewDTO getApplicantReviewData(String applicationId, String organiserId) {
        return getApplicantReviewData(applicationId, organiserId, false);
    }

    public ApplicantReviewDTO getApplicantReviewData(String applicationId,
                                                     String actorUserId,
                                                     boolean adminOverride) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));
        if (!hasReviewPermission(job, actorUserId, adminOverride)) {
            throw new IllegalArgumentException("Permission denied.");
        }

        String applicantId = application.getApplicantId();
        String applicantName = profileRepository.findById(applicantId)
                .map(profile -> profile.getFullName())
                .orElse(applicantId);
        ResumeInfo resume = resumeInfoRepository.findByApplicantId(applicantId).orElse(new ResumeInfo());
        WorkloadSummaryDTO workload = workloadService.getWorkload(applicantId);
        int projected = workloadService.calculateProjectedHours(applicantId, job.getJobId());

        return new ApplicantReviewDTO(
                applicationId,
                applicantId,
                applicantName,
                resume.getTechnicalSkills() == null ? List.of() : resume.getTechnicalSkills(),
                resume.getAvailability() == null ? List.of() : resume.getAvailability(),
                workload.currentHours(),
                projected,
                workload.maxWeeklyHours(),
                workloadService.calculateRiskLevel(projected, workload.maxWeeklyHours()).name(),
                application.getStatement(),
                application.getDecisionNote(),
                application.getMatchScore(),
                application.getMissingSkills() == null ? List.of() : application.getMissingSkills()
        );
    }

    private boolean hasReviewPermission(Job job, String actorUserId, boolean adminOverride) {
        return adminOverride || actorUserId != null && actorUserId.equals(job.getOrganiserId());
    }
}
