package com.game.songlash;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor

public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRegistry playerRegistry;

    private void sendError(String sessionId, String text){
        var error = ChatMessage.builder()
                .type(MessageType.ERROR)
                .content(text)
                .build();

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", error, accessor.getMessageHeaders());
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage in, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        String requested = in.getSender() == null ? "" : in.getSender().trim();

        boolean valid = (!requested.isEmpty() && requested.length()<=10);
        if (!valid) {
            sendError(sessionId, "invalid player name");
            return;
        }

        if(playerRegistry.find(sessionId).isPresent()){
            sendError(sessionId, "you already joined");
            return;
        }

        Optional<Player> maybePlayer = playerRegistry.add(sessionId, requested);
        if (maybePlayer.isEmpty()){
            sendError(sessionId, "name is taken already");
            return;
        }
        Player player = maybePlayer.get();

        messagingTemplate.convertAndSend("/topic/public", ChatMessage.builder()
                .type(MessageType.JOIN)
                .sender(player.name())
                .players(playerRegistry.names())
                .build());
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage in, SimpMessageHeaderAccessor accessor){
        String sessionId = accessor.getSessionId();

        Optional<Player> maybePlayer = playerRegistry.find(sessionId);
        if(maybePlayer.isEmpty()){
            sendError(sessionId, "join before sending a message");
            return;
        }
        Player player = maybePlayer.get();

        String content = in.getContent() == null ? "" : in.getContent().trim();
        if(content.isEmpty() || content.length() > 200) return;

        messagingTemplate.convertAndSend("/topic/public", ChatMessage.builder()
                .type(MessageType.CHAT)
                .sender(player.name())
                .content(content)
                .build());
    }


}
