package service;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.UserRepository;
import edu.bupt.ta.service.ApplicantProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApplicantProfileServiceTest {

    @TempDir
    Path tempDir;

    private ApplicantProfileService service;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        service = new ApplicantProfileService(new ApplicantProfileRepository(), new UserRepository());
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldGetOrCreateProfile() {
        ApplicantProfile profile = service.getOrCreateProfile("U001");
        assertNotNull(profile.getApplicantId());
        assertEquals("U001", profile.getUserId());
    }

    @Test
    void shouldValidateProfileFields() {
        ApplicantProfile profile = service.getOrCreateProfile("U001");
        profile.setEmail("bad-email");
        var result = service.saveProfile(profile);
        assertFalse(result.isValid());
    }
}
