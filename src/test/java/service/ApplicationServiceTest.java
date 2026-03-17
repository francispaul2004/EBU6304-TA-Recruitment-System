package service;

import edu.bupt.ta.repository.*;
import edu.bupt.ta.service.ApplicationService;
import edu.bupt.ta.service.MatchingService;
import edu.bupt.ta.service.WorkloadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationServiceTest {

    @TempDir
    Path tempDir;

    private ApplicationService service;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);

        ApplicationRepository appRepo = new ApplicationRepository();
        JobRepository jobRepo = new JobRepository();
        ApplicantProfileRepository profileRepo = new ApplicantProfileRepository();
        ResumeInfoRepository resumeRepo = new ResumeInfoRepository();
        AuditLogRepository auditRepo = new AuditLogRepository();
        WorkloadService workloadService = new WorkloadService(new WorkloadRepository(), appRepo, jobRepo, resumeRepo);
        MatchingService matchingService = new MatchingService(resumeRepo, jobRepo, workloadService);

        service = new ApplicationService(appRepo, jobRepo, profileRepo, resumeRepo, auditRepo, matchingService);
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldApplySuccessfully() {
        var result = service.apply("A001", "J002", "I can assist labs.");
        assertTrue(result.isValid());
        assertFalse(service.getApplicationsByApplicant("A001").isEmpty());
    }

    @Test
    void shouldBlockDuplicateApplication() {
        assertFalse(service.apply("A001", "J001", "duplicate").isValid());
    }
}
