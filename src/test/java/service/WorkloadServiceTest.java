package service;

import edu.bupt.ta.enums.RiskLevel;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.repository.WorkloadRepository;
import edu.bupt.ta.service.WorkloadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadServiceTest {

    @TempDir
    Path tempDir;

    private WorkloadService workloadService;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        workloadService = new WorkloadService(
                new WorkloadRepository(),
                new ApplicationRepository(),
                new JobRepository(),
                new ResumeInfoRepository()
        );
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldCalculateRiskLevels() {
        assertEquals(RiskLevel.LOW, workloadService.calculateRiskLevel(6, 10));
        assertEquals(RiskLevel.MEDIUM, workloadService.calculateRiskLevel(9, 10));
        assertEquals(RiskLevel.HIGH, workloadService.calculateRiskLevel(11, 10));
    }

    @Test
    void shouldRefreshWorkloadFromAcceptedApplications() {
        workloadService.refreshWorkloadForApplicant("A004");
        var workload = workloadService.getWorkload("A004");
        assertEquals(5, workload.currentHours());
    }
}
