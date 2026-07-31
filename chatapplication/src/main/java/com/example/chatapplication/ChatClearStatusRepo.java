package com.example.chatapplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatClearStatusRepo extends JpaRepository<ChatClearStatus, Long> {
    Optional<ChatClearStatus> findByConnectionIdAndUserEmail(Long connectionId, String userEmail);
    void deleteByConnectionId(Long connectionId);
    void deleteByUserEmail(String userEmail);
}
