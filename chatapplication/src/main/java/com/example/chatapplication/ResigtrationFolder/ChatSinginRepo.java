package com.example.chatapplication.ResigtrationFolder;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSinginRepo extends JpaRepository<ChatSingin, Long> {
    Optional<ChatSingin> findByuseremail(String useremail);
    Optional<ChatSingin> findByusername(String username);
}
