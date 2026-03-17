package service;

import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.repository.WorkloadRepository;
import edu.bupt.ta.service.MatchingService;
import edu.bupt.ta.service.WorkloadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {

    @TempDir
    Path tempDir;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        JobRepository jobRepository = new JobRepository();
        ResumeInfoRepository resumeInfoRepository = new ResumeInfoRepository();
        WorkloadService workloadService = new WorkloadService(new WorkloadRepository(),
                new ApplicationRepository(), jobRepository, resumeInfoRepository);
        matchingService = new MatchingService(resumeInfoRepository, jobRepository, workloadService);
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldReturnExplainableMatch() {
        var dto = matchingService.evaluateMatch("A001", "J005");
        assertTrue(dto.score() >= 0 && dto.score() <= 100);
        assertTrue(dto.explanation().contains("projected workload"));
        assertTrue(dto.missingSkills().contains("Python"));
    }
}
