package com.example.chatapplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverEmailAndStatus(String receiverEmail, String status);
    boolean existsBySenderEmailAndReceiverEmailAndStatus(String senderEmail, String receiverEmail, String status);
    void deleteBySenderEmailOrReceiverEmail(String senderEmail, String receiverEmail);
}
