package edu.bupt.ta.service;

import edu.bupt.ta.dto.WorkloadSummaryDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.Workload;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.repository.WorkloadRepository;

import java.util.List;
import java.util.Optional;

public class WorkloadService {

    private final WorkloadRepository workloadRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeInfoRepository resumeInfoRepository;

    public WorkloadService(WorkloadRepository workloadRepository,
                           ApplicationRepository applicationRepository,
                           JobRepository jobRepository,
                           ResumeInfoRepository resumeInfoRepository) {
        this.workloadRepository = workloadRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeInfoRepository = resumeInfoRepository;
    }

    public WorkloadSummaryDTO getWorkload(String applicantId) {
        Workload workload = workloadRepository.findByApplicantId(applicantId).orElseGet(() -> {
            Workload newW = new Workload();
            newW.setApplicantId(applicantId);
            newW.setRiskLevel(RiskLevel.LOW);
            workloadRepository.save(newW);
            return newW;
        });

        int maxWeeklyHours = resumeInfoRepository.findByApplicantId(applicantId)
                .map(resume -> resume.getMaxWeeklyHours())
                .orElse(8);
        return new WorkloadSummaryDTO(applicantId, workload.getCurrentWeeklyHours(), maxWeeklyHours,
                workload.getCurrentWeeklyHours(), workload.getRiskLevel());
    }

    public int calculateProjectedHours(String applicantId, String jobId) {
        int current = workloadRepository.findByApplicantId(applicantId).map(Workload::getCurrentWeeklyHours).orElse(0);
        int incoming = jobRepository.findById(jobId).map(Job::getWeeklyHours).orElse(0);
        return current + incoming;
    }

    public RiskLevel calculateRiskLevel(int projectedHours, int maxWeeklyHours) {
        if (maxWeeklyHours <= 0) {
            return RiskLevel.HIGH;
        }
        double ratio = projectedHours * 1.0 / maxWeeklyHours;
        if (ratio <= 0.8) {
            return RiskLevel.LOW;
        }
        if (ratio <= 1.0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }

    public boolean willExceedMaxHours(String applicantId, String jobId) {
        int projected = calculateProjectedHours(applicantId, jobId);
        int max = resumeInfoRepository.findByApplicantId(applicantId)
                .map(resume -> resume.getMaxWeeklyHours())
                .orElse(8);
        return projected > max;
    }

    public void refreshWorkloadForApplicant(String applicantId) {
        List<Application> acceptedApplications = applicationRepository.findByApplicantId(applicantId).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .toList();

        int currentHours = 0;
        List<String> acceptedJobIds = acceptedApplications.stream().map(Application::getJobId).distinct().toList();
        for (String jobId : acceptedJobIds) {
            currentHours += jobRepository.findById(jobId).map(Job::getWeeklyHours).orElse(0);
        }

        int maxWeeklyHours = resumeInfoRepository.findByApplicantId(applicantId)
                .map(resume -> resume.getMaxWeeklyHours())
                .orElse(8);
        RiskLevel riskLevel = calculateRiskLevel(currentHours, maxWeeklyHours);

        Workload workload = workloadRepository.findByApplicantId(applicantId).orElseGet(Workload::new);
        workload.setApplicantId(applicantId);
        workload.setAcceptedJobIds(acceptedJobIds);
        workload.setCurrentWeeklyHours(currentHours);
        workload.setRiskLevel(riskLevel);
        workloadRepository.save(workload);
    }

    public List<Workload> findAll() {
        return workloadRepository.findAll();
    }
}
