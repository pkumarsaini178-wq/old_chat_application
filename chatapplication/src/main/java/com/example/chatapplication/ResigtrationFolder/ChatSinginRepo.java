package com.example.chatapplication.ResigtrationFolder;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSinginRepo extends JpaRepository<ChatSingin, Long> {
    Optional<ChatSingin> findByuseremail(String useremail);
    Optional<ChatSingin> findByusername(String username);

    Optional<ChatSingin> findFirstByUseremailIgnoreCase(String useremail);
    Optional<ChatSingin> findFirstByUsernameIgnoreCase(String username);
    List<ChatSingin> findAllByUseremailIgnoreCase(String useremail);
}
