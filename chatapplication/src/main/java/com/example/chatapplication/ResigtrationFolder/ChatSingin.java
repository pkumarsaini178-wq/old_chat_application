package com.example.chatapplication.ResigtrationFolder;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ChatSingin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String currentpassword;
    private String useremail;
    private Boolean isBlocked = false;
    private java.time.LocalDateTime blockExpiry;
    private String role = "USER";
}
