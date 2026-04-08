package edu.bupt.ta.service;

import edu.bupt.ta.dto.AdminApplicationRowDTO;
import edu.bupt.ta.dto.AdminDashboardSummaryDTO;
import edu.bupt.ta.dto.AdminJobRowDTO;
import edu.bupt.ta.dto.AdminWorkloadRowDTO;
import edu.bupt.ta.dto.AuditLogItemDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.model.Workload;
import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.AuditLogRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.repository.UserRepository;
import edu.bupt.ta.repository.WorkloadRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminMonitoringService {

    private final UserRepository userRepository;
    private final ApplicantProfileRepository applicantProfileRepository;
    private final ResumeInfoRepository resumeInfoRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final WorkloadRepository workloadRepository;
    private final AuditLogRepository auditLogRepository;
    private final JobService jobService;
    private final WorkloadService workloadService;

    public AdminMonitoringService(UserRepository userRepository,
                                  ApplicantProfileRepository applicantProfileRepository,
                                  ResumeInfoRepository resumeInfoRepository,
                                  JobRepository jobRepository,
                                  ApplicationRepository applicationRepository,
                                  WorkloadRepository workloadRepository,
                                  AuditLogRepository auditLogRepository,
                                  JobService jobService,
                                  WorkloadService workloadService) {
        this.userRepository = userRepository;
        this.applicantProfileRepository = applicantProfileRepository;
        this.resumeInfoRepository = resumeInfoRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.workloadRepository = workloadRepository;
        this.auditLogRepository = auditLogRepository;
        this.jobService = jobService;
        this.workloadService = workloadService;
    }

    public AdminDashboardSummaryDTO getDashboardSummary() {
        jobService.refreshExpiredStatuses();
        long totalJobs = jobRepository.findAll().size();
        long totalApplications = applicationRepository.findAll().size();
        long acceptedApplications = applicationRepository.findAll().stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        long highRiskApplicants = workloadRepository.findAll().stream()
                .filter(workload -> workload.getRiskLevel() == RiskLevel.HIGH)
                .count();
        return new AdminDashboardSummaryDTO(totalJobs, totalApplications, acceptedApplications, highRiskApplicants);
    }

    public List<AdminWorkloadRowDTO> getWorkloadRows() {
        Map<String, ApplicantProfile> profiles = profilesByApplicantId();
        Map<String, User> users = usersByUserId();

        return workloadRepository.findAll().stream()
                .map(workload -> buildWorkloadRow(workload, profiles, users))
                .sorted(Comparator
                        .comparingInt((AdminWorkloadRowDTO row) -> riskPriority(row.riskLevel()))
                        .thenComparing(AdminWorkloadRowDTO::currentWeeklyHours, Comparator.reverseOrder())
                        .thenComparing(AdminWorkloadRowDTO::applicantName))
                .toList();
    }

    public List<AdminJobRowDTO> getJobRows() {
        jobService.refreshExpiredStatuses();
        Map<String, User> users = usersByUserId();

        return jobRepository.findAll().stream()
                .map(job -> buildJobRow(job, users))
                .sorted(Comparator
                        .comparing(AdminJobRowDTO::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminJobRowDTO::title))
                .toList();
    }

    public List<AdminApplicationRowDTO> getApplicationRows() {
        jobService.refreshExpiredStatuses();
        Map<String, ApplicantProfile> profiles = profilesByApplicantId();
        Map<String, User> users = usersByUserId();

        return applicationRepository.findAll().stream()
                .map(application -> buildApplicationRow(application, profiles, users))
                .sorted(Comparator
                        .comparing(AdminApplicationRowDTO::applyDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminApplicationRowDTO::applicationId))
                .toList();
    }

    public List<AuditLogItemDTO> getAuditLogs() {
        Map<String, User> users = usersByUserId();
        return auditLogRepository.findAllLines().stream()
                .map(line -> parseAuditLine(line, users))
                .sorted(Comparator
                        .comparing((AuditLogItemDTO item) -> item.timestamp() == null ? LocalDateTime.MIN : item.timestamp())
                        .reversed())
                .toList();
    }

    private AdminWorkloadRowDTO buildWorkloadRow(Workload workload,
                                                 Map<String, ApplicantProfile> profiles,
                                                 Map<String, User> users) {
        ApplicantProfile profile = profiles.get(workload.getApplicantId());
        ResumeInfo resume = resumeInfoRepository.findByApplicantId(workload.getApplicantId()).orElse(new ResumeInfo());
        String applicantName = applicantName(workload.getApplicantId(), profile, users);
        String studentId = profile == null || blank(profile.getStudentId()) ? "-" : profile.getStudentId();
        int acceptedJobs = workload.getAcceptedJobIds() == null ? 0 : workload.getAcceptedJobIds().size();
        int maxWeeklyHours = Math.max(resume.getMaxWeeklyHours(), 0);
        String riskLevel = workload.getRiskLevel() == null ? "-" : workload.getRiskLevel().name();

        return new AdminWorkloadRowDTO(
                workload.getApplicantId(),
                applicantName,
                studentId,
                acceptedJobs,
                workload.getCurrentWeeklyHours(),
                maxWeeklyHours,
                riskLevel,
                buildWorkloadNote(workload.getRiskLevel(), acceptedJobs)
        );
    }

    private AdminJobRowDTO buildJobRow(Job job, Map<String, User> users) {
        String organiserId = blank(job.getOrganiserId()) ? "-" : job.getOrganiserId();
        String organiserName = users.getOrDefault(job.getOrganiserId(), fallbackUser(organiserId)).getDisplayName();
        int applicantCount = applicationRepository.findByJobId(job.getJobId()).size();

        return new AdminJobRowDTO(
                job.getJobId(),
                blankToDash(job.getTitle()),
                blankToDash(job.getModuleCode()),
                blankToDash(job.getModuleName()),
                blankToDash(job.getSemester()),
                organiserId,
                blankToDash(organiserName),
                job.getType() == null ? "-" : job.getType().name(),
                job.getStatus() == null ? "-" : job.getStatus().name(),
                job.getWeeklyHours(),
                job.getPositions(),
                job.getDeadline(),
                job.getCreatedAt(),
                applicantCount,
                safeList(job.getRequiredSkills()),
                safeList(job.getPreferredSkills()),
                blankToDash(job.getDescription())
        );
    }

    private AdminApplicationRowDTO buildApplicationRow(Application application,
                                                       Map<String, ApplicantProfile> profiles,
                                                       Map<String, User> users) {
        ApplicantProfile profile = profiles.get(application.getApplicantId());
        String applicantName = applicantName(application.getApplicantId(), profile, users);
        String studentId = profile == null || blank(profile.getStudentId()) ? "-" : profile.getStudentId();

        Optional<Job> jobOpt = jobRepository.findById(application.getJobId());
        String organiserId = jobOpt.map(Job::getOrganiserId).filter(value -> !blank(value)).orElse("-");
        String organiserName = users.getOrDefault(organiserId, fallbackUser(organiserId)).getDisplayName();

        int currentHours = workloadService.getWorkload(application.getApplicantId()).currentHours();
        Workload currentWorkload = workloadRepository.findByApplicantId(application.getApplicantId()).orElse(null);
        boolean alreadyCounted = currentWorkload != null
                && currentWorkload.getAcceptedJobIds() != null
                && currentWorkload.getAcceptedJobIds().contains(application.getJobId());
        int projectedHours = jobOpt.map(job -> alreadyCounted
                        ? currentHours
                        : workloadService.calculateProjectedHours(application.getApplicantId(), job.getJobId()))
                .orElse(currentHours);
        int maxWeeklyHours = workloadService.getWorkload(application.getApplicantId()).maxWeeklyHours();
        String riskLevel = workloadService.calculateRiskLevel(projectedHours, maxWeeklyHours).name();

        return new AdminApplicationRowDTO(
                application.getApplicationId(),
                application.getJobId(),
                jobOpt.map(Job::getTitle).filter(value -> !blank(value)).orElse(application.getJobId()),
                application.getApplicantId(),
                applicantName,
                studentId,
                organiserId,
                blankToDash(organiserName),
                application.getStatus() == null ? "-" : application.getStatus().name(),
                application.getApplyDate(),
                application.getMatchScore(),
                currentHours,
                projectedHours,
                riskLevel,
                blankToDash(application.getDecisionNote()),
                blankToDash(application.getStatement()),
                safeList(application.getMissingSkills())
        );
    }

    private AuditLogItemDTO parseAuditLine(String line, Map<String, User> users) {
        String[] parts = line == null ? new String[0] : line.split("\\s*\\|\\s*", 4);
        if (parts.length < 4) {
            return new AuditLogItemDTO(null, "-", "-", "RAW", blankToDash(line));
        }

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(parts[0]);
        } catch (Exception ex) {
            timestamp = null;
        }

        String actorUserId = blankToDash(parts[1]);
        String actorName = users.getOrDefault(actorUserId, fallbackUser(actorUserId)).getDisplayName();
        return new AuditLogItemDTO(timestamp, actorUserId, blankToDash(actorName), blankToDash(parts[2]), blankToDash(parts[3]));
    }

    private Map<String, ApplicantProfile> profilesByApplicantId() {
        Map<String, ApplicantProfile> map = new HashMap<>();
        for (ApplicantProfile profile : applicantProfileRepository.findAll()) {
            map.put(profile.getApplicantId(), profile);
        }
        return map;
    }

    private Map<String, User> usersByUserId() {
        Map<String, User> map = new HashMap<>();
        for (User user : userRepository.findAll()) {
            map.put(user.getUserId(), user);
        }
        return map;
    }

    private String applicantName(String applicantId, ApplicantProfile profile, Map<String, User> users) {
        if (profile != null && !blank(profile.getFullName())) {
            return profile.getFullName();
        }
        if (profile != null && !blank(profile.getUserId())) {
            User user = users.get(profile.getUserId());
            if (user != null && !blank(user.getDisplayName())) {
                return user.getDisplayName();
            }
        }
        return applicantId;
    }

    private String buildWorkloadNote(RiskLevel riskLevel, int acceptedJobs) {
        if (riskLevel == RiskLevel.HIGH) {
            return "Requires immediate review";
        }
        if (riskLevel == RiskLevel.MEDIUM) {
            return "Close to policy limit";
        }
        if (acceptedJobs == 0) {
            return "Available for more tasks";
        }
        return "Healthy workload";
    }

    private int riskPriority(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }

    private User fallbackUser(String userId) {
        User fallback = new User();
        fallback.setDisplayName(blankToDash(userId));
        return fallback;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String blankToDash(String value) {
        return blank(value) ? "-" : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
