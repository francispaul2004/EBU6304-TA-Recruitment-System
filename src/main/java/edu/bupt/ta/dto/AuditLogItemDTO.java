package edu.bupt.ta.dto;

import java.time.LocalDateTime;

public record AuditLogItemDTO(LocalDateTime timestamp,
                              String actorUserId,
                              String actorName,
                              String action,
                              String detail) {
}
