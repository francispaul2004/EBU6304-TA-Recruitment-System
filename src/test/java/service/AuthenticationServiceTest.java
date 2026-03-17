package service;

import edu.bupt.ta.service.AuthenticationService;
import edu.bupt.ta.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {

    @TempDir
    Path tempDir;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        authenticationService = new AuthenticationService(new UserRepository());
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldLoginSuccessfully() {
        var result = authenticationService.login("ta001", "Password123!");
        assertTrue(result.success());
        assertNotNull(result.user());
    }

    @Test
    void shouldRejectInvalidPassword() {
        var result = authenticationService.login("ta001", "wrong");
        assertFalse(result.success());
        assertTrue(result.message().contains("Incorrect"));
    }
}
