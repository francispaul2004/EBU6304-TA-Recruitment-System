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
import java.util.ArrayList;
import java.util.Locale;
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

        ValidationResult statusCheck = validateStatusTransition(application.getStatus(), ApplicationStatus.ACCEPTED);
        if (!statusCheck.isValid()) {
            return statusCheck;
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

        ValidationResult statusCheck = validateStatusTransition(application.getStatus(), ApplicationStatus.REJECTED);
        if (!statusCheck.isValid()) {
            return statusCheck;
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

        String decisionNote = application.getDecisionNote() == null ? "" : application.getDecisionNote();

        MatchExplanationDTO match = getMatchExplanationIfAvailable(applicantId, job.getJobId()).orElse(null);
        List<String> matchedSkills = (match == null)
            ? deriveMatchedSkills(job.getRequiredSkills(), application.getMissingSkills())
            : safeList(match.matchedSkills());
        String matchExplanation = (match == null)
            ? fallbackMatchExplanation(matchedSkills, application.getMissingSkills(), projected)
            : (match.explanation() == null ? "" : match.explanation());

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
            application.getMissingSkills() == null ? List.of() : application.getMissingSkills(),
            matchedSkills,
            matchExplanation,
            decisionNote
        );
    }

    private boolean hasReviewPermission(Job job, String actorUserId, boolean adminOverride) {
        return adminOverride || actorUserId != null && actorUserId.equals(job.getOrganiserId());
    private ValidationResult validateStatusTransition(ApplicationStatus current, ApplicationStatus target) {
        if (target == null) {
            return ValidationResult.fail("Target status is required.");
        }

        if (current == null) {
            return ValidationResult.fail("Current status is missing.");
        }

        if (current == target) {
            return ValidationResult.fail("No status change: application is already " + target.name() + ".");
        }

        // Business rules:
        // - SUBMITTED -> UNDER_REVIEW (handled elsewhere)
        // - UNDER_REVIEW -> ACCEPTED / REJECTED
        // - REJECTED -> ACCEPTED allowed
        // - ACCEPTED -> REJECTED not allowed
        return switch (target) {
            case ACCEPTED -> {
                if (current == ApplicationStatus.ACCEPTED) {
                    yield ValidationResult.fail("Application is already ACCEPTED.");
                }
                if (current == ApplicationStatus.SUBMITTED
                        || current == ApplicationStatus.UNDER_REVIEW
                        || current == ApplicationStatus.REJECTED) {
                    yield ValidationResult.ok();
                }
                yield ValidationResult.fail("Invalid status transition from " + current.name() + " to ACCEPTED.");
            }
            case REJECTED -> {
                if (current == ApplicationStatus.ACCEPTED) {
                    yield ValidationResult.fail("Cannot reject an ACCEPTED application.");
                }
                if (current == ApplicationStatus.REJECTED) {
                    yield ValidationResult.fail("Application is already REJECTED.");
                }
                if (current == ApplicationStatus.SUBMITTED || current == ApplicationStatus.UNDER_REVIEW) {
                    yield ValidationResult.ok();
                }
                yield ValidationResult.fail("Invalid status transition from " + current.name() + " to REJECTED.");
            }
            case SUBMITTED, UNDER_REVIEW -> ValidationResult.fail(
                    "Direct transition to " + target.name() + " is not supported here.");
        };
    }

    private Optional<MatchExplanationDTO> getMatchExplanationIfAvailable(String applicantId, String jobId) {
        if (matchingService == null) {
            return Optional.empty();
        }
        return Optional.of(matchingService.evaluateMatch(applicantId, jobId));
    }

    private List<String> deriveMatchedSkills(List<String> requiredSkills, List<String> missingSkills) {
        List<String> required = safeList(requiredSkills);
        List<String> missing = safeList(missingSkills);

        List<String> matched = new ArrayList<>();
        for (String req : required) {
            if (req == null || req.isBlank()) {
                continue;
            }
            boolean isMissing = missing.stream().anyMatch(m -> equalsIgnoreCaseTrimmed(m, req));
            if (!isMissing) {
                matched.add(req);
            }
        }
        return matched;
    }

    private String fallbackMatchExplanation(List<String> matchedSkills, List<String> missingSkills, int projectedHours) {
        int matchedCount = safeList(matchedSkills).size();
        int missingCount = safeList(missingSkills).size();
        return "Required skills matched " + matchedCount + ", missing " + missingCount
                + "; projected workload " + projectedHours + "h/week.";
    }

    private boolean equalsIgnoreCaseTrimmed(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
