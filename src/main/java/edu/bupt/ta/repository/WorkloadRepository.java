package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.Workload;

import java.util.Optional;

public class WorkloadRepository extends AbstractJsonRepository<Workload, String> {

    public WorkloadRepository() {
        super(AppPaths.workloadsJson(), Workload.class);
    }

    public Optional<Workload> findByApplicantId(String applicantId) {
        return findById(applicantId);
    }
}
