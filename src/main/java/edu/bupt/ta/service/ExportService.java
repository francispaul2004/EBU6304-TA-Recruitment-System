package edu.bupt.ta.service;

import com.opencsv.CSVWriter;
import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Workload;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.WorkloadRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExportService {

    private final WorkloadRepository workloadRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ExportService(WorkloadRepository workloadRepository,
                         ApplicationRepository applicationRepository,
                         JobRepository jobRepository) {
        this.workloadRepository = workloadRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    public Path exportWorkloadReport() {
        try {
            Files.createDirectories(AppPaths.exportDir());
            Path target = AppPaths.workloadExportCsv();
            try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(target))) {
                writer.writeNext(new String[]{"applicantId", "currentWeeklyHours", "riskLevel", "acceptedJobIds"});
                for (Workload workload : workloadRepository.findAll()) {
                    writer.writeNext(new String[]{
                            workload.getApplicantId(),
                            String.valueOf(workload.getCurrentWeeklyHours()),
                            String.valueOf(workload.getRiskLevel()),
                            String.join(";", workload.getAcceptedJobIds())
                    });
                }
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export workload report.", e);
        }
    }

    public Path exportApplicationReport() {
        try {
            Files.createDirectories(AppPaths.exportDir());
            Path target = AppPaths.applicationExportCsv();
            try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(target))) {
                writer.writeNext(new String[]{"applicationId", "jobId", "jobTitle", "applicantId", "status", "matchScore"});
                for (Application application : applicationRepository.findAll()) {
                    writer.writeNext(new String[]{
                            application.getApplicationId(),
                            application.getJobId(),
                            jobRepository.findById(application.getJobId()).map(job -> job.getTitle()).orElse(""),
                            application.getApplicantId(),
                            String.valueOf(application.getStatus()),
                            String.valueOf(application.getMatchScore())
                    });
                }
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export application report.", e);
        }
    }

    public long countAcceptedApplications() {
        return applicationRepository.findAll().stream().filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED).count();
    }
}
