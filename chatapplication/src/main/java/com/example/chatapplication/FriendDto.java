package com.example.chatapplication;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto {
    private Long id; // connectionId
    private Long connectionId;
    private String user1Email;
    private String user2Email;
    private String friendEmail;
    private String friendName;
    private Boolean isOnline;
    private String lastSeen;
}
