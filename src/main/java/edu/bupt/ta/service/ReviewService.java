package edu.bupt.ta.service;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.dto.MatchExplanationDTO;
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
    private final MatchingService matchingService;

    public ReviewService(ApplicationRepository applicationRepository,
                         JobRepository jobRepository,
                         ApplicantProfileRepository profileRepository,
                         ResumeInfoRepository resumeInfoRepository,
                         WorkloadService workloadService,
                         AuditLogRepository auditLogRepository) {
        this(applicationRepository, jobRepository, profileRepository, resumeInfoRepository,
                workloadService, auditLogRepository, null);
    }

    public ReviewService(ApplicationRepository applicationRepository,
                         JobRepository jobRepository,
                         ApplicantProfileRepository profileRepository,
                         ResumeInfoRepository resumeInfoRepository,
                         WorkloadService workloadService,
                         AuditLogRepository auditLogRepository,
                         MatchingService matchingService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.resumeInfoRepository = resumeInfoRepository;
        this.workloadService = workloadService;
        this.auditLogRepository = auditLogRepository;
        this.matchingService = matchingService;
    }

    public ValidationResult acceptApplication(String applicationId, String organiserId, String decisionNote) {
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
        if (!organiserId.equals(job.getOrganiserId())) {
            return ValidationResult.fail("Permission denied: not your job.");
        }

        ValidationResult statusCheck = validateStatusTransition(application.getStatus(), ApplicationStatus.ACCEPTED);
        if (!statusCheck.isValid()) {
            return statusCheck;
        }

        application.setStatus(ApplicationStatus.ACCEPTED);
        application.setDecisionNote(decisionNote == null ? "" : decisionNote.trim());
        applicationRepository.save(application);

        workloadService.refreshWorkloadForApplicant(application.getApplicantId());
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), organiserId,
                "ACCEPT_APPLICATION", applicationId + " accepted"));
        return ValidationResult.ok();
    }

    public ValidationResult rejectApplication(String applicationId, String organiserId, String decisionNote) {
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
        if (!organiserId.equals(job.getOrganiserId())) {
            return ValidationResult.fail("Permission denied: not your job.");
        }

        ValidationResult statusCheck = validateStatusTransition(application.getStatus(), ApplicationStatus.REJECTED);
        if (!statusCheck.isValid()) {
            return statusCheck;
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setDecisionNote(decisionNote == null ? "" : decisionNote.trim());
        applicationRepository.save(application);

        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), organiserId,
                "REJECT_APPLICATION", applicationId + " rejected"));
        return ValidationResult.ok();
    }

    public ApplicantReviewDTO getApplicantReviewData(String applicationId, String organiserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));
        if (!organiserId.equals(job.getOrganiserId())) {
            throw new IllegalArgumentException("Permission denied.");
        }

        if (application.getStatus() == ApplicationStatus.SUBMITTED) {
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            applicationRepository.save(application);
            auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), organiserId,
                "MARK_UNDER_REVIEW", applicationId + " moved to UNDER_REVIEW"));
        }

        String applicantId = application.getApplicantId();
        String applicantName = profileRepository.findById(applicantId)
                .map(profile -> profile.getFullName())
                .orElse(applicantId);
        ResumeInfo resume = resumeInfoRepository.findByApplicantId(applicantId).orElse(new ResumeInfo());
        WorkloadSummaryDTO workload = workloadService.getWorkload(applicantId);
        int projected = workloadService.calculateProjectedHours(applicantId, job.getJobId());

        // Prepare match explanation retrieval without changing DTO/controller in this step.
        // This will be used once the UI/DTO chain is extended.
        getMatchExplanationIfAvailable(applicantId, job.getJobId());

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
                application.getMatchScore(),
                application.getMissingSkills() == null ? List.of() : application.getMissingSkills()
        );
    }

    private ValidationResult validateStatusTransition(ApplicationStatus current, ApplicationStatus target) {
        if (current == ApplicationStatus.ACCEPTED) {
            return ValidationResult.fail("Application is already ACCEPTED.");
        }
        if (current == ApplicationStatus.REJECTED) {
            return ValidationResult.fail("Application is already REJECTED.");
        }
        if (current != ApplicationStatus.SUBMITTED && current != ApplicationStatus.UNDER_REVIEW) {
            return ValidationResult.fail("Invalid status transition from " + (current == null ? "null" : current.name())
                    + " to " + target.name() + ".");
        }
        return ValidationResult.ok();
    }

    private Optional<MatchExplanationDTO> getMatchExplanationIfAvailable(String applicantId, String jobId) {
        if (matchingService == null) {
            return Optional.empty();
        }
        return Optional.of(matchingService.evaluateMatch(applicantId, jobId));
    }
}
