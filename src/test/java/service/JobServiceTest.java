package service;

import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.repository.AuditLogRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JobServiceTest {

    @TempDir
    Path tempDir;

    private JobService service;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        service = new JobService(new JobRepository(), new AuditLogRepository());
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldCreateValidJob() {
        Job job = new Job();
        job.setTitle("Test Job");
        job.setModuleCode("TS101");
        job.setModuleName("Testing");
        job.setType(JobType.MODULE_TA);
        job.setWeeklyHours(3);
        job.setPositions(1);
        job.setDeadline(LocalDate.now().plusDays(7));
        job.setOrganiserId("U101");
        job.setStatus(JobStatus.OPEN);

        assertTrue(service.createJob(job).isValid());
    }

    @Test
    void shouldRejectInvalidJob() {
        Job job = new Job();
        assertFalse(service.createJob(job).isValid());
    }
}
