package edu.bupt.ta.service;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.ValidationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ResumeService {
    private static final long MAX_CV_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

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

    public ValidationResult uploadCvFile(String applicantId, Path sourceFile) {
        if (sourceFile == null || !Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            return ValidationResult.fail(List.of("Please select a valid file."));
        }
        String originalName = sourceFile.getFileName().toString();
        String extension = extensionOf(originalName);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            return ValidationResult.fail(List.of("Only PDF and DOCX files are supported."));
        }
        try {
            long size = Files.size(sourceFile);
            if (size <= 0 || size > MAX_CV_FILE_SIZE_BYTES) {
                return ValidationResult.fail(List.of("File size must be between 1 byte and 10MB."));
            }

            Path targetDir = AppPaths.dataDir().resolve("cv").resolve(applicantId);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve("cv." + extension);
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            ResumeInfo resume = getOrCreateResume(applicantId);
            resume.setCvFileName(originalName);
            resume.setCvStoredPath(targetFile.toString());
            resume.setCvFileSizeBytes(size);
            resume.setCvUploadedAt(DateTimeUtils.now());
            resume.setLastUpdated(DateTimeUtils.now());
            resumeInfoRepository.save(resume);
            return ValidationResult.ok();
        } catch (IOException e) {
            return ValidationResult.fail(List.of("Failed to save CV file: " + e.getMessage()));
        }
    }

    public ValidationResult deleteCvFile(String applicantId) {
        ResumeInfo resume = getOrCreateResume(applicantId);
        String storedPath = resume.getCvStoredPath();
        try {
            if (storedPath != null && !storedPath.isBlank()) {
                Files.deleteIfExists(Path.of(storedPath));
            }
            resume.setCvFileName(null);
            resume.setCvStoredPath(null);
            resume.setCvFileSizeBytes(0L);
            resume.setCvUploadedAt(null);
            resume.setLastUpdated(DateTimeUtils.now());
            resumeInfoRepository.save(resume);
            return ValidationResult.ok();
        } catch (IOException e) {
            return ValidationResult.fail(List.of("Failed to delete CV file: " + e.getMessage()));
        }
    }

    public Optional<Path> getCvFilePath(String applicantId) {
        ResumeInfo resume = getOrCreateResume(applicantId);
        if (resume.getCvStoredPath() == null || resume.getCvStoredPath().isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(resume.getCvStoredPath());
        return Files.exists(path) ? Optional.of(path) : Optional.empty();
    }

    private String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
