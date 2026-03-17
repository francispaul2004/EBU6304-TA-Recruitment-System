package edu.bupt.ta.config;

import java.nio.file.Path;

public final class AppPaths {

    private AppPaths() {
    }

    public static Path dataDir() {
        String override = System.getProperty("ta.data.dir");
        return (override == null || override.isBlank()) ? Path.of("data") : Path.of(override);
    }

    public static Path usersJson() {
        return dataDir().resolve("users.json");
    }

    public static Path applicantProfilesJson() {
        return dataDir().resolve("applicant_profiles.json");
    }

    public static Path resumeInfosJson() {
        return dataDir().resolve("resume_infos.json");
    }

    public static Path jobsJson() {
        return dataDir().resolve("jobs.json");
    }

    public static Path applicationsJson() {
        return dataDir().resolve("applications.json");
    }

    public static Path workloadsJson() {
        return dataDir().resolve("workloads.json");
    }

    public static Path auditLogTxt() {
        return dataDir().resolve("audit_log.txt");
    }

    public static Path exportDir() {
        return dataDir().resolve("export");
    }

    public static Path workloadExportCsv() {
        return exportDir().resolve("workload_report.csv");
    }

    public static Path applicationExportCsv() {
        return exportDir().resolve("application_report.csv");
    }
}
