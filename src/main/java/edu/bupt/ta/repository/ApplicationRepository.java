package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.Application;

import java.util.List;
import java.util.Optional;

public class ApplicationRepository extends AbstractJsonRepository<Application, String> {

    public ApplicationRepository() {
        super(AppPaths.applicationsJson(), Application.class);
    }

    public List<Application> findByApplicantId(String applicantId) {
        return findAll().stream()
                .filter(app -> app.getApplicantId() != null && app.getApplicantId().equals(applicantId))
                .toList();
    }

    public List<Application> findByJobId(String jobId) {
        return findAll().stream()
                .filter(app -> app.getJobId() != null && app.getJobId().equals(jobId))
                .toList();
    }

    public Optional<Application> findByJobIdAndApplicantId(String jobId, String applicantId) {
        return findAll().stream()
                .filter(app -> app.getJobId() != null && app.getJobId().equals(jobId)
                        && app.getApplicantId() != null && app.getApplicantId().equals(applicantId))
                .findFirst();
    }
}
