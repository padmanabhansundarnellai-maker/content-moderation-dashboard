package com.example.mod;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepo users;
    private final PostRepo posts;
    private final PasswordEncoder encoder;

    private User make(String name, String pass, String role) {
        User u = new User();
        u.setUsername(name);
        u.setPassword(encoder.encode(pass));
        u.setRole(role);
        return users.save(u);
    }

    private void post(User author, String text) {
        Post p = new Post();
        p.setAuthor(author);
        p.setContent(text);
        posts.save(p);
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        make("admin", "admin123", "ADMIN");
        make("mod", "mod123", "MODERATOR");
        User alice = make("alice", "alice123", "USER");
        User bob = make("bob", "bob123", "USER");
        post(alice, "Welcome everyone! Happy to join this community.");
        post(bob, "Get FREE MONEY now, click this scam link!!!");
        post(alice, "You are such an idiot, nobody wants your opinion.");
        post(bob, "Does anyone have tips for learning Spring Boot?");
        post(alice, "Buy cheap followers today, limited offer, DM me.");
        post(bob, "This new policy is great, thanks for sharing.");
    }
}
