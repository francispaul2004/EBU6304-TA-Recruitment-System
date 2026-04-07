package service;

import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.service.ResumeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeServiceTest {

    @TempDir
    Path tempDir;

    private ResumeService service;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        service = new ResumeService(new ResumeInfoRepository());
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldGetOrCreateResume() {
        ResumeInfo resume = service.getOrCreateResume("A001");
        assertEquals("A001", resume.getApplicantId());
    }

    @Test
    void shouldRejectInvalidResume() {
        ResumeInfo resume = service.getOrCreateResume("A001");
        resume.setAvailability(List.of());
        resume.setMaxWeeklyHours(0);
        var result = service.saveResume(resume);
        assertFalse(result.isValid());
    }

    @Test
    void shouldUploadAndDeleteCvFile() throws Exception {
        Path source = tempDir.resolve("demo-cv.pdf");
        Files.writeString(source, "demo-cv-content");

        var uploadResult = service.uploadCvFile("A001", source);
        assertTrue(uploadResult.isValid());

        ResumeInfo resume = service.getOrCreateResume("A001");
        assertEquals("demo-cv.pdf", resume.getCvFileName());
        assertNotNull(resume.getCvStoredPath());
        assertTrue(Files.exists(Path.of(resume.getCvStoredPath())));

        var deleteResult = service.deleteCvFile("A001");
        assertTrue(deleteResult.isValid());
        assertNull(service.getOrCreateResume("A001").getCvStoredPath());
    }
}
