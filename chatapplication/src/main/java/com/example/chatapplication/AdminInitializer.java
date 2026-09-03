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

    @Override
    public void run(String... args) throws Exception {
        // Ensure ONLY java71932@gmail.com is the Single Admin
        createOrUpdateAdmin(admin1Email, admin1Password, "pankaj");

        // Completely delete default user pkumarsaini178@gmail.com from the database
        Optional<ChatSingin> oldAdmin2 = chatSinginRepo.findByuseremail("pkumarsaini178@gmail.com");
        if (oldAdmin2.isPresent()) {
            chatSinginRepo.delete(oldAdmin2.get());
        }
    }

    private void createOrUpdateAdmin(String email, String rawPassword, String defaultUsername) {
        if (email == null || email.trim().isEmpty())
            return;
        String cleanEmail = email.trim().toLowerCase();

        Optional<ChatSingin> existingOpt = chatSinginRepo.findByuseremail(cleanEmail);
        if (existingOpt.isPresent()) {
            ChatSingin adminUser = existingOpt.get();
            adminUser.setRole("ADMIN");
            adminUser.setIsBlocked(false);
            adminUser.setBlockExpiry(null);
            // Only set default admin password if password is missing
            if (adminUser.getPassword() == null || adminUser.getPassword().trim().isEmpty()) {
                adminUser.setPassword(passwordEncoder.encode(rawPassword));
                adminUser.setCurrentpassword(passwordEncoder.encode(rawPassword));
            }
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
