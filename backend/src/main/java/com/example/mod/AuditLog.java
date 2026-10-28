package com.example.mod;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "audit_log") @Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String moderator;
    private String action;
    private Long reportId;
    private Long postId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
