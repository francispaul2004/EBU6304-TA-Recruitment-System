package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Job;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class JobRepository extends AbstractJsonRepository<Job, String> {

    public JobRepository() {
        super(AppPaths.jobsJson(), Job.class);
    }

    public List<Job> findByOrganiserId(String organiserId) {
        return findAll().stream()
                .filter(job -> Objects.equals(job.getOrganiserId(), organiserId))
                .toList();
    }

    public List<Job> findOpenJobs() {
        return findAll().stream().filter(job -> job.getStatus() == JobStatus.OPEN).toList();
    }

    public List<Job> search(JobSearchCriteria criteria) {
        Stream<Job> stream = findAll().stream();

        if (criteria == null) {
            return stream.toList();
        }

        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            String key = criteria.getKeyword().toLowerCase(Locale.ROOT);
            stream = stream.filter(job -> containsIgnoreCase(job.getTitle(), key)
                    || containsIgnoreCase(job.getModuleCode(), key)
                    || containsIgnoreCase(job.getModuleName(), key)
                    || containsIgnoreCase(job.getDescription(), key));
        }

        if (criteria.getModuleCode() != null && !criteria.getModuleCode().isBlank()) {
            String moduleCode = criteria.getModuleCode().toLowerCase(Locale.ROOT);
            stream = stream.filter(job -> containsIgnoreCase(job.getModuleCode(), moduleCode));
        }

        if (criteria.getType() != null) {
            stream = stream.filter(job -> job.getType() == criteria.getType());
        }

        if (criteria.getRequiredSkill() != null && !criteria.getRequiredSkill().isBlank()) {
            String skill = criteria.getRequiredSkill().toLowerCase(Locale.ROOT);
            stream = stream.filter(job -> job.getRequiredSkills().stream()
                    .anyMatch(s -> s != null && s.toLowerCase(Locale.ROOT).contains(skill)));
        }

        if (criteria.getStatus() != null) {
            stream = stream.filter(job -> job.getStatus() == criteria.getStatus());
        }

        if (criteria.getMaxWeeklyHours() != null) {
            stream = stream.filter(job -> job.getWeeklyHours() <= criteria.getMaxWeeklyHours());
        }

        return stream.toList();
    }

    private boolean containsIgnoreCase(String value, String key) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(key);
    }
}
