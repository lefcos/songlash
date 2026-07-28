package com.game.songlash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j //SImple logging facade for java

public class WebSocketEventListener {

    private final SimpMessageSendingOperations messageTemplate;
    private final PlayerRegistry playerRegistry;
    private final RoomManager roomManager;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        playerRegistry.remove(sessionId).ifPresent(player -> {
            log.info("player disconnected: {}", player.name());

            player.roomId().ifPresent(code -> {
                Room room = roomManager.getRoom(code);
                if (room != null) {
                    room.getPlayers().removeIf(p -> p.sessionId().equals(sessionId));

                    if (room.getPlayers().isEmpty()) {
                        roomManager.removeRoom(code);
                    }
                }
            });

            messageTemplate.convertAndSend("/topic/public", ChatMessage.builder()
                    .type(MessageType.LEAVE)
                    .sender(player.name())
                    .players(playerRegistry.names())
                    .build());
        });
    }
}