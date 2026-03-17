package edu.bupt.ta.service;

import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class ResumeService {

    private final ResumeInfoRepository resumeInfoRepository;

    public ResumeService(ResumeInfoRepository resumeInfoRepository) {
        this.resumeInfoRepository = resumeInfoRepository;
    }

    public ResumeInfo getOrCreateResume(String applicantId) {
        return resumeInfoRepository.findByApplicantId(applicantId).orElseGet(() -> {
            ResumeInfo resume = new ResumeInfo();
            resume.setApplicantId(applicantId);
            resume.setMaxWeeklyHours(8);
            resume.setLastUpdated(DateTimeUtils.now());
            resumeInfoRepository.save(resume);
            return resume;
        });
    }

    public ValidationResult saveResume(ResumeInfo resumeInfo) {
        List<String> errors = new ArrayList<>();
        if (resumeInfo.getMaxWeeklyHours() <= 0) {
            errors.add("maxWeeklyHours must be greater than 0");
        }
        if (resumeInfo.getAvailability() == null || resumeInfo.getAvailability().isEmpty()) {
            errors.add("at least one availability is required");
        }
        if (resumeInfo.getPersonalStatement() != null && resumeInfo.getPersonalStatement().length() > 500) {
            errors.add("personalStatement must be <= 500 chars");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.fail(errors);
        }

        resumeInfo.setLastUpdated(DateTimeUtils.now());
        resumeInfoRepository.save(resumeInfo);
        return ValidationResult.ok();
    }

    public List<String> getMissingResumeSections(String applicantId) {
        ResumeInfo resume = getOrCreateResume(applicantId);
        List<String> missing = new ArrayList<>();
        if (resume.getRelevantModules() == null || resume.getRelevantModules().isEmpty()) {
            missing.add("relevantModules");
        }
        if (resume.getTechnicalSkills() == null || resume.getTechnicalSkills().isEmpty()) {
            missing.add("technicalSkills");
        }
        if (resume.getAvailability() == null || resume.getAvailability().isEmpty()) {
            missing.add("availability");
        }
        if (resume.getPersonalStatement() == null || resume.getPersonalStatement().isBlank()) {
            missing.add("personalStatement");
        }
        return missing;
    }

    public int calculateResumeCompletion(String applicantId) {
        ResumeInfo resume = getOrCreateResume(applicantId);
        int total = 7;
        int complete = 0;
        if (resume.getRelevantModules() != null && !resume.getRelevantModules().isEmpty()) complete++;
        if (resume.getTechnicalSkills() != null && !resume.getTechnicalSkills().isEmpty()) complete++;
        if (resume.getLanguageSkills() != null && !resume.getLanguageSkills().isEmpty()) complete++;
        if (resume.getExperienceText() != null && !resume.getExperienceText().isBlank()) complete++;
        if (resume.getPersonalStatement() != null && !resume.getPersonalStatement().isBlank()) complete++;
        if (resume.getAvailability() != null && !resume.getAvailability().isEmpty()) complete++;
        if (resume.getMaxWeeklyHours() > 0) complete++;
        return complete * 100 / total;
    }
}
