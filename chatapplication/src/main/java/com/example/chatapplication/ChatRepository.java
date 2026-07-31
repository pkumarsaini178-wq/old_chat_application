package com.example.chatapplication;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
    java.util.List<ChatEntity> findByConnectionIdOrderByTimestampAsc(Long connectionId);

    java.util.List<ChatEntity> findByConnectionIdOrderByTimestampDesc(Long connectionId, Pageable pageable);

    void deleteByConnectionId(Long connectionId);
    void deleteBySender(String sender);
}
