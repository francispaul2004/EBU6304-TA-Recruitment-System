package repository;

import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.User;
import edu.bupt.ta.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonRepositoryTest {

    @TempDir
    Path tempDir;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        TestDataSupport.prepareData(tempDir);
        userRepository = new UserRepository();
    }

    @AfterEach
    void tearDown() {
        TestDataSupport.clearDataDirOverride();
    }

    @Test
    void shouldSaveAndDeleteEntity() {
        User user = new User("UT99", "tester", "hash", Role.TA, "Tester", true);
        userRepository.save(user);

        assertTrue(userRepository.findById("UT99").isPresent());

        userRepository.deleteById("UT99");
        assertTrue(userRepository.findById("UT99").isEmpty());
    }
}
