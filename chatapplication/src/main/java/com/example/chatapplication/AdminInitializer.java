package com.example.chatapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.chatapplication.ResigtrationFolder.ChatSingin;
import com.example.chatapplication.ResigtrationFolder.ChatSinginRepo;

import java.util.Optional;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private ChatSinginRepo chatSinginRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.user1.email:java71932@gmail.com}")
    private String admin1Email;

    @Value("${admin.user1.password:2215795455#@$}")
    private String admin1Password;

    @Value("${admin.user2.email:pkumarsaini178@gmail.com}")
    private String admin2Email;

    @Value("${admin.user2.password:2215795455#@$}")
    private String admin2Password;

    @Override
    public void run(String... args) throws Exception {
        createOrUpdateAdmin(admin1Email, admin1Password, "java71932");
        createOrUpdateAdmin(admin2Email, admin2Password, "pkumarsaini178");
    }

    private void createOrUpdateAdmin(String email, String rawPassword, String defaultUsername) {
        if (email == null || email.trim().isEmpty()) return;
        String cleanEmail = email.trim().toLowerCase();

        Optional<ChatSingin> existingOpt = chatSinginRepo.findByuseremail(cleanEmail);
        if (existingOpt.isPresent()) {
            ChatSingin adminUser = existingOpt.get();
            adminUser.setRole("ADMIN");
            adminUser.setIsBlocked(false);
            adminUser.setBlockExpiry(null);
            adminUser.setPassword(passwordEncoder.encode(rawPassword));
            adminUser.setCurrentpassword(passwordEncoder.encode(rawPassword));
            chatSinginRepo.save(adminUser);
        } else {
            ChatSingin newAdmin = new ChatSingin();
            newAdmin.setUsername(defaultUsername);
            newAdmin.setUseremail(cleanEmail);
            newAdmin.setPassword(passwordEncoder.encode(rawPassword));
            newAdmin.setCurrentpassword(passwordEncoder.encode(rawPassword));
            newAdmin.setRole("ADMIN");
            newAdmin.setIsBlocked(false);
            chatSinginRepo.save(newAdmin);
        }
    }
}
