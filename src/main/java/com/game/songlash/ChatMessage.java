package com.game.songlash;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ChatMessage {
    private String content;
    private String sender;
    private MessageType type;
    private List<String> players;
}
