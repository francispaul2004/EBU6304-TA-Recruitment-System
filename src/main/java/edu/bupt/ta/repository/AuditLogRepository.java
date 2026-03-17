package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.AuditLogEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class AuditLogRepository {

    private final Path filePath;

    public AuditLogRepository() {
        this.filePath = AppPaths.auditLogTxt();
        ensureFileExists();
    }

    public void append(AuditLogEntry entry) {
        try {
            Files.writeString(filePath, entry.toLine() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append audit log.", e);
        }
    }

    public List<String> findAllLines() {
        try {
            if (!Files.exists(filePath)) {
                return List.of();
            }
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read audit logs.", e);
        }
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize audit log file.", e);
        }
    }
}
