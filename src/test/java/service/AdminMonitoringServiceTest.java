package service;

import edu.bupt.ta.service.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDataSupport;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminMonitoringServiceTest {

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
    void shouldBuildAdminDashboardSummaryAndRows() {
        var summary = services.adminMonitoringService().getDashboardSummary();

        assertEquals(8, summary.totalJobs());
        assertEquals(10, summary.totalApplications());
        assertEquals(2, summary.acceptedApplications());
        assertEquals(1, summary.highRiskApplicants());
        assertEquals(5, services.adminMonitoringService().getWorkloadRows().size());
        assertEquals(8, services.adminMonitoringService().getJobRows().size());
        assertEquals(10, services.adminMonitoringService().getApplicationRows().size());
        assertFalse(services.adminMonitoringService().getAuditLogs().isEmpty());
    }
}
