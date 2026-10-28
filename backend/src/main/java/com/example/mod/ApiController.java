package com.example.mod;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final UserRepo users;
    private final PostRepo posts;
    private final ReportRepo reports;
    private final AuditRepo audits;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Value("${app.keywords}")
    private String[] keywords;

    private static final Set<String> CATS =
        Set.of("SPAM", "HARASSMENT", "HATE_SPEECH", "MISINFORMATION", "OTHER");

    // ---------- helpers ----------
    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private String blank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private String flagged(String text) {
        String t = text.toLowerCase();
        for (String k : keywords) if (t.contains(k.trim().toLowerCase())) return k.trim();
        return null;
    }

    private User current(Authentication a) {
        return users.findByUsername(a.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Dto.ReportView view(Report r) {
        Post p = r.getPost();
        return new Dto.ReportView(r.getId(), p.getId(), p.getContent(), p.getAuthor().getUsername(),
            r.getReporter().getUsername(), r.getCategory(), r.getReason(), r.getStatus(),
            r.getPriority(), r.getFlaggedKeyword(), r.getResolvedBy(), r.getCreatedAt());
    }

    // ---------- auth ----------
    @PostMapping("/auth/register")
    public Dto.AuthResp register(@RequestBody Dto.Auth r) {
        if (r.username() == null || r.username().isBlank() || r.password() == null || r.password().length() < 4)
            throw bad("Username required and password must be at least 4 characters");
        if (users.findByUsername(r.username().trim()).isPresent()) throw bad("Username already taken");
        User u = new User();
        u.setUsername(r.username().trim());
        u.setPassword(encoder.encode(r.password()));
        u.setRole("USER");
        users.save(u);
        return new Dto.AuthResp(jwt.generate(u.getUsername(), "USER"), u.getUsername(), "USER");
    }

    @PostMapping("/auth/login")
    public Dto.AuthResp login(@RequestBody Dto.Auth r) {
        if (r.username() == null || r.password() == null) throw bad("Username and password required");
        User u = users.findByUsername(r.username().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(r.password(), u.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        return new Dto.AuthResp(jwt.generate(u.getUsername(), u.getRole()), u.getUsername(), u.getRole());
    }

    // ---------- posts (any logged-in user) ----------
    @GetMapping("/posts")
    public List<Dto.PostView> listPosts() {
        return posts.findByStatusOrderByCreatedAtDesc("VISIBLE").stream()
            .map(p -> new Dto.PostView(p.getId(), p.getAuthor().getUsername(), p.getContent(), p.getCreatedAt()))
            .toList();
    }

    @PostMapping("/posts")
    public Dto.PostView createPost(@RequestBody Dto.PostReq r, Authentication a) {
        if (r.content() == null || r.content().isBlank() || r.content().length() > 1000)
            throw bad("Content required (max 1000 chars)");
        Post p = new Post();
        p.setAuthor(current(a));
        p.setContent(r.content().trim());
        posts.save(p);
        return new Dto.PostView(p.getId(), p.getAuthor().getUsername(), p.getContent(), p.getCreatedAt());
    }

    @PostMapping("/posts/{id}/report")
    public Dto.ReportView report(@PathVariable Long id, @RequestBody Dto.ReportReq q, Authentication a) {
        Post p = posts.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (q.category() == null || !CATS.contains(q.category())) throw bad("Invalid category");
        Report r = new Report();
        r.setPost(p);
        r.setReporter(current(a));
        r.setCategory(q.category());
        r.setReason(q.reason());
        String kw = flagged(p.getContent() + " " + (q.reason() == null ? "" : q.reason()));
        r.setFlaggedKeyword(kw);
        r.setPriority(kw != null ? 1 : 0);
        reports.save(r);
        return view(r);
    }

    // ---------- moderation (MODERATOR / ADMIN) ----------
    @GetMapping("/moderation/reports")
    public Map<String, Object> listReports(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "5") int size) {
        Pageable pg = PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("priority"), Sort.Order.asc("createdAt")));
        Page<Report> res = reports.search(blank(status), blank(category), pg);
        return Map.of("items", res.getContent().stream().map(this::view).toList(),
                      "page", res.getNumber(),
                      "totalPages", res.getTotalPages(),
                      "totalElements", res.getTotalElements());
    }

    @PostMapping("/moderation/reports/{id}/resolve")
    @Transactional
    public Dto.ReportView resolve(@PathVariable Long id, @RequestBody Dto.ResolveReq q, Authentication a) {
        Report r = reports.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (!"OPEN".equals(r.getStatus())) throw bad("Report already resolved");
        String act = q.action() == null ? "" : q.action();
        switch (act) {
            case "APPROVE" -> r.setStatus("APPROVED");
            case "REMOVE" -> {
                r.setStatus("REMOVED");
                r.getPost().setStatus("REMOVED");
                posts.save(r.getPost());
            }
            case "WARN" -> {
                r.setStatus("WARNED");
                User author = r.getPost().getAuthor();
                author.setWarnings(author.getWarnings() + 1);
                users.save(author);
            }
            default -> throw bad("Action must be APPROVE, REMOVE or WARN");
        }
        r.setResolvedBy(a.getName());
        reports.save(r);

        AuditLog log = new AuditLog();
        log.setModerator(a.getName());
        log.setAction(act);
        log.setReportId(r.getId());
        log.setPostId(r.getPost().getId());
        audits.save(log);
        return view(r);
    }

    @GetMapping("/moderation/audit")
    public Map<String, Object> audit(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Page<AuditLog> res = audits.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return Map.of("items", res.getContent(), "page", res.getNumber(), "totalPages", res.getTotalPages());
    }

    // ---------- admin analytics ----------
    @GetMapping("/admin/analytics")
    public Map<String, Object> analytics() {
        Map<String, Long> byCat = new LinkedHashMap<>();
        for (Object[] o : reports.countByCategory()) byCat.put((String) o[0], (Long) o[1]);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] o : reports.countByStatus()) byStatus.put((String) o[0], (Long) o[1]);
        return Map.of("total", reports.count(),
                      "flagged", reports.countByPriority(1),
                      "byCategory", byCat,
                      "byStatus", byStatus);
    }
}
