package com.example.mod;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepo extends JpaRepository<Post, Long> {
    List<Post> findByStatusOrderByCreatedAtDesc(String status);
}
