package com.game.songlash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j

public class WebSocketEventListener {

    private final SimpMessageSendingOperations messageTemplate;
    private final PlayerRegistry playerRegistry;

    @EventListener
    public void handleWebSocketDisconnectListener(
            SessionDisconnectEvent event
    ){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headerAccessor.getSessionId();
        playerRegistry.remove(sessionId).ifPresent(player -> {
            log.info("Player disconnected: {}", player.name());
            messageTemplate.convertAndSend("/topic/public", ChatMessage.builder()
                    .type(MessageType.LEAVE)
                    .sender(player.name())
                    .players(playerRegistry.names())   // AFTER the remove
                    .build());
        });
    }
}
