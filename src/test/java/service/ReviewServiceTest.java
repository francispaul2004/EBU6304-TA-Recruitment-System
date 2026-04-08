package service;

import edu.bupt.ta.repository.*;
import edu.bupt.ta.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReviewServiceTest {

    @TempDir
    Path tempDir;

    private ReviewService reviewService;
    private ApplicationRepository applicationRepository;
    private WorkloadRepository workloadRepository;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);

        applicationRepository = new ApplicationRepository();
        JobRepository jobRepository = new JobRepository();
        ApplicantProfileRepository profileRepository = new ApplicantProfileRepository();
        ResumeInfoRepository resumeInfoRepository = new ResumeInfoRepository();
        AuditLogRepository auditLogRepository = new AuditLogRepository();
        workloadRepository = new WorkloadRepository();

        WorkloadService workloadService = new WorkloadService(workloadRepository, applicationRepository, jobRepository, resumeInfoRepository);
        reviewService = new ReviewService(applicationRepository, jobRepository, profileRepository, resumeInfoRepository,
                workloadService, auditLogRepository);
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldAcceptApplicationAndRefreshWorkload() {
        var result = reviewService.acceptApplication("APP001", "U101", "Good fit");
        assertTrue(result.isValid());
        assertEquals("ACCEPTED", applicationRepository.findById("APP001").orElseThrow().getStatus().name());
        assertTrue(workloadRepository.findByApplicantId("A001").isPresent());
    }

    @Test
    void shouldRejectWhenOrganiserDoesNotOwnJob() {
        var result = reviewService.rejectApplication("APP001", "U102", "No permission");
        assertFalse(result.isValid());
    }

    @Test
    void shouldAllowAdminToRejectApplicationAcrossJobs() {
        var result = reviewService.rejectApplication("APP001", "U900", "Admin override", true);
        assertTrue(result.isValid());
        assertEquals("REJECTED", applicationRepository.findById("APP001").orElseThrow().getStatus().name());
    }
}
