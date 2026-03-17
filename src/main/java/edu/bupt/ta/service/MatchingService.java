package edu.bupt.ta.service;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatchingService {

    private final ResumeInfoRepository resumeInfoRepository;
    private final JobRepository jobRepository;
    private final WorkloadService workloadService;

    public MatchingService(ResumeInfoRepository resumeInfoRepository,
                           JobRepository jobRepository,
                           WorkloadService workloadService) {
        this.resumeInfoRepository = resumeInfoRepository;
        this.jobRepository = jobRepository;
        this.workloadService = workloadService;
    }

    public MatchExplanationDTO evaluateMatch(String applicantId, String jobId) {
        ResumeInfo resume = resumeInfoRepository.findByApplicantId(applicantId).orElse(new ResumeInfo());
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found."));

        int score = calculateMatchScore(resume, job);
        List<String> missing = findMissingSkills(resume, job);
        List<String> matched = findMatchedSkills(resume, job);

        int current = workloadService.getWorkload(applicantId).currentHours();
        int projected = workloadService.calculateProjectedHours(applicantId, jobId);

        String explanation = "Required skills matched " + matched.size() + ", missing " + missing.size()
                + "; projected workload " + projected + "h/week.";

        return new MatchExplanationDTO(score, matched, missing, current, projected, explanation);
    }

    public int calculateMatchScore(ResumeInfo resume, Job job) {
        List<String> technical = safeList(resume.getTechnicalSkills());
        List<String> required = safeList(job.getRequiredSkills());
        List<String> preferred = safeList(job.getPreferredSkills());

        double requiredRatio = ratio(technical, required);
        double preferredRatio = ratio(technical, preferred);

        int requiredScore = (int) Math.round(requiredRatio * 60);
        int preferredScore = (int) Math.round(preferredRatio * 20);

        int moduleExperienceScore = 0;
        boolean moduleMatched = safeList(resume.getRelevantModules()).stream().anyMatch(module -> containsIgnoreCase(module, job.getModuleCode())
                || containsIgnoreCase(module, job.getModuleName()));
        if (moduleMatched || (resume.getExperienceText() != null && !resume.getExperienceText().isBlank())) {
            moduleExperienceScore = 10;
        }

        int availabilityScore = (resume.getAvailability() != null && !resume.getAvailability().isEmpty()) ? 10 : 0;

        int total = requiredScore + preferredScore + moduleExperienceScore + availabilityScore;
        return Math.max(0, Math.min(100, total));
    }

    public List<String> findMissingSkills(ResumeInfo resume, Job job) {
        List<String> technical = safeList(resume.getTechnicalSkills());
        List<String> required = safeList(job.getRequiredSkills());
        List<String> missing = new ArrayList<>();
        for (String req : required) {
            if (technical.stream().noneMatch(skill -> containsIgnoreCase(skill, req))) {
                missing.add(req);
            }
        }
        return missing;
    }

    private List<String> findMatchedSkills(ResumeInfo resume, Job job) {
        List<String> technical = safeList(resume.getTechnicalSkills());
        List<String> required = safeList(job.getRequiredSkills());
        List<String> matched = new ArrayList<>();
        for (String req : required) {
            if (technical.stream().anyMatch(skill -> containsIgnoreCase(skill, req))) {
                matched.add(req);
            }
        }
        return matched;
    }

    private double ratio(List<String> technical, List<String> targetSkills) {
        if (targetSkills.isEmpty()) {
            return 1.0;
        }
        long matched = targetSkills.stream().filter(req -> technical.stream().anyMatch(skill -> containsIgnoreCase(skill, req))).count();
        return matched * 1.0 / targetSkills.size();
    }

    private boolean containsIgnoreCase(String left, String right) {
        return left != null && right != null && left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
