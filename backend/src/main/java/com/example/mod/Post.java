package com.example.mod;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "posts") @Getter @Setter @NoArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 1000)
    private String content;
    @ManyToOne
    private User author;
    private String status = "VISIBLE";   // VISIBLE, REMOVED
    private LocalDateTime createdAt = LocalDateTime.now();
}
