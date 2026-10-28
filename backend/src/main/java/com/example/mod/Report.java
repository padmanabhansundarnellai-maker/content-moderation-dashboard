package com.example.mod;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "reports") @Getter @Setter @NoArgsConstructor
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Post post;
    @ManyToOne
    private User reporter;
    private String category;          // SPAM, HARASSMENT, HATE_SPEECH, MISINFORMATION, OTHER
    @Column(length = 500)
    private String reason;
    private String status = "OPEN";   // OPEN, APPROVED, REMOVED, WARNED
    private int priority;             // 1 = keyword flagged
    private String flaggedKeyword;
    private String resolvedBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
