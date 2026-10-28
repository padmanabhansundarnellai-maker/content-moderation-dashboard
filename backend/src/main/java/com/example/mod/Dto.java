package com.example.mod;

import java.time.LocalDateTime;

public class Dto {
    public record Auth(String username, String password) {}
    public record AuthResp(String token, String username, String role) {}
    public record PostReq(String content) {}
    public record ReportReq(String category, String reason) {}
    public record ResolveReq(String action) {}
    public record PostView(Long id, String author, String content, LocalDateTime createdAt) {}
    public record ReportView(Long id, Long postId, String postContent, String postAuthor, String reporter,
                             String category, String reason, String status, int priority,
                             String flaggedKeyword, String resolvedBy, LocalDateTime createdAt) {}
}
