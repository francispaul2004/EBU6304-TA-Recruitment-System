package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.ResumeInfo;

import java.util.Optional;

public class ResumeInfoRepository extends AbstractJsonRepository<ResumeInfo, String> {

    public ResumeInfoRepository() {
        super(AppPaths.resumeInfosJson(), ResumeInfo.class);
    }

    public Optional<ResumeInfo> findByApplicantId(String applicantId) {
        return findById(applicantId);
    }
}
