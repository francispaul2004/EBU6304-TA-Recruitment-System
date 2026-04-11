package integration;

import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.service.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationFlowTest {

    @TempDir
    Path tempDir;

    private ServiceRegistry services;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        services = new ServiceRegistry();
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void createJobSaveReload() {
        Job job = new Job();
        job.setTitle("Integration Job");
        job.setModuleCode("IT100");
        job.setModuleName("Integration Testing");
        job.setType(JobType.MODULE_TA);
        job.setWeeklyHours(2);
        job.setPositions(1);
        job.setDeadline(LocalDateTime.now().plusDays(10));
        job.setOrganiserId("U101");
        job.setStatus(JobStatus.OPEN);

        assertTrue(services.jobService().createJob(job).isValid());
        assertTrue(services.jobRepository().findAll().stream().anyMatch(j -> "Integration Job".equals(j.getTitle())));
    }

    @Test
    void applyJobApplicationVisible() {
        assertTrue(services.applicationService().apply("A001", "J002", "integration apply").isValid());
        assertTrue(services.applicationRepository().findByJobIdAndApplicantId("J002", "A001").isPresent());
    }

    @Test
    void acceptApplicationWorkloadUpdated() {
        assertTrue(services.reviewService().acceptApplication("APP001", "U101", "ok").isValid());
        assertTrue(services.workloadRepository().findByApplicantId("A001").orElseThrow().getCurrentWeeklyHours() > 0);
    }

    @Test
    void closeJobNoFurtherApplication() {
        services.jobService().closeJob("J002", "U101");
        assertFalse(services.applicationService().apply("A003", "J002", "late").isValid());
    }

    @Test
    void expiredJobBlocked() {
        assertFalse(services.applicationService().apply("A003", "J004", "expired").isValid());
    }

    @Test
    void exportCsvGenerated() throws Exception {
        Path workload = services.exportService().exportWorkloadReport();
        Path app = services.exportService().exportApplicationReport();
        assertTrue(Files.exists(workload));
        assertTrue(Files.exists(app));
    }
}
