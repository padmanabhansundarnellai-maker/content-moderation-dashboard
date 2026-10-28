package com.example.mod;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    private String password;
    private String role;      // USER, MODERATOR, ADMIN
    private int warnings;
}
