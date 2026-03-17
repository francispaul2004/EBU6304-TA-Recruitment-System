package support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class TestDataSupport {

    private TestDataSupport() {
    }

    public static void prepareData(Path tempDir) throws IOException {
        Path source = Path.of("src/main/resources/sample-data");
        Files.createDirectories(tempDir);
        for (String name : List.of(
                "users.json",
                "applicant_profiles.json",
                "resume_infos.json",
                "jobs.json",
                "applications.json",
                "workloads.json",
                "audit_log.txt",
                "workload_report.csv",
                "application_report.csv"
        )) {
            Path src = source.resolve(name);
            if (name.endsWith(".csv")) {
                Files.createDirectories(tempDir.resolve("export"));
                Path target = tempDir.resolve("export").resolve(name);
                Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Path target = tempDir.resolve(name);
                Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        System.setProperty("ta.data.dir", tempDir.toString());
    }

    public static void clearDataDirOverride() {
        System.clearProperty("ta.data.dir");
    }
}
