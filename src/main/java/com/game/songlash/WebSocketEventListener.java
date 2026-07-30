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
            String display = player.name().orElse("anonymous");
            log.info("player disconnected: {}", player.name());

            player.roomId().ifPresent(code -> {
                Room room = roomManager.getRoom(code);
                if (room != null) {
                    room.getSessionIds().remove(sessionId);

                    player.name().ifPresent(name ->
                            messageTemplate.convertAndSend("/topic/room." + code, GameMessage.builder()
                                    .type(MessageType.LEAVE)
                                    .sender(name)
                                    .content(name + " left the room")
                                    .build())
                    );

                    if (room.getSessionIds().isEmpty()) {
                        roomManager.removeRoom(code);
                    }
                }
            });
        });
    }
}