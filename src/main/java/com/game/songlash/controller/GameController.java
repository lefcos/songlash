package com.game.songlash.controller;

import com.game.songlash.exception.GameException;
import com.game.songlash.model.GameMessage;
import com.game.songlash.model.MessageType;
import com.game.songlash.model.Player;
import com.game.songlash.model.Room;
import com.game.songlash.repository.PlayerRegistry;
import com.game.songlash.repository.RoomManager;
import com.game.songlash.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j

public class GameController {
    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRegistry playerRegistry;
    private final RoomManager roomManager;
    private final GameService gameService;

    private record PlayerInRoom(Player player, Room room) {}

    @MessageMapping("/game.createRoom")
    public void createRoom(SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        Room room = gameService.createRoom(sessionId);

        GameMessage messageCreated = GameMessage.builder()
                .type(MessageType.ROOM_CREATED)
                .sender("system")
                .content("room created with code " + room.getCode())
                .players(playerRegistry.namesInRoom(room.getCode()))
                .host(true)
                .hostName(hostNameOf(room))
                .build();
        sendToUser(sessionId, "/queue/room", messageCreated);
    }

    @MessageMapping("/game.joinRoom")
    public void joinRoom(@Payload GameMessage in, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        Room room = gameService.joinRoom(sessionId, in.getContent());

        GameMessage message = GameMessage.builder()
                .type(MessageType.ROOM_JOINED)
                .sender("system")
                .content("joined room with code " + room.getCode())
                .players(playerRegistry.namesInRoom(room.getCode()))
                .host(room.isHostSession(sessionId))
                .hostName(hostNameOf(room))
                .build();
        sendToUser(sessionId, "/queue/room", message);
    }

    @MessageMapping("/game.setName")
    public void setName(@Payload GameMessage in, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();
        String requested = in.getSender() == null ? "" : in.getSender().trim();

        if(requested.length() >= 12 || requested.length() < 2){
            throw new GameException("name must be between 2 and 12 characters");
        }

        Room room = requirePlayerInRoom(sessionId).room();

        if (playerRegistry.isNameTaken(room.getCode(), requested)) {
            throw new GameException("name is taken in this room");
        }

        playerRegistry.setPlayerName(sessionId, requested);

        sendToRoom(room.getCode(), GameMessage.builder()
                .type(MessageType.JOIN)
                .sender(requested)
                .content(requested + " joined the room")
                .players(playerRegistry.namesInRoom(room.getCode()))
                .hostName(hostNameOf(room))
                .build());
    }

    @MessageMapping("/game.startGame")
    public void startGame(SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        Room room = requirePlayerInRoom(sessionId).room();

        if (!room.isHostSession(sessionId)) {
            throw new GameException("only the host can start the game");
        }

        // TODO: game state logic
        sendToRoom(room.getCode(), GameMessage.builder()
                .type(MessageType.START_GAME)
                .sender("system")
                .content("game started")
                .build());
    }

    @MessageMapping("/game.roomMessage")
    public void roomMessage(@Payload GameMessage in, SimpMessageHeaderAccessor accessor){
        String sessionId = accessor.getSessionId();

        PlayerInRoom context = requirePlayerInRoom(sessionId);

        if (context.player().name().isEmpty()) {
            throw new GameException("set your name first");
        }

        String content = in.getContent() == null ? "" : in.getContent().trim();
        if(content.isEmpty() || content.length() > 200){
            return; //ignore silently, not worth an error
        }

        sendToRoom(context.room().getCode(), GameMessage.builder()
                .type(MessageType.CHAT)
                .sender(context.player().name().get())
                .content(content)
                .build());
    }

    @MessageExceptionHandler(GameException.class)
    public void handleGameException(GameException ex, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();
        log.debug("rejected request from {}: {}", sessionId, ex.getMessage());
        sendToUser(sessionId, "/queue/errors", GameMessage.builder()
                .type(MessageType.ERROR)
                .content(ex.getMessage())
                .build());
    }

    //helper funcs
    private void sendToUser(String sessionId, String destination, Object payload) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload, accessor.getMessageHeaders());
    }
    private void sendToRoom(String roomCode, Object payload) {
        messagingTemplate.convertAndSend("/topic/room." + roomCode, payload);
    }
    private String hostNameOf(Room room) {
        return playerRegistry.find(room.getHostSessionId())
                .flatMap(Player::name)
                .orElse("Host");
    }
    private PlayerInRoom requirePlayerInRoom(String sessionId) {
        Player player = playerRegistry.find(sessionId)
                .filter(p -> p.roomId().isPresent())
                .orElseThrow(() -> new GameException("join a room first"));

        Room room = roomManager.getRoom(player.roomId().get());
        if (room == null) {
            throw new GameException("room does not exist");
        }
        return new PlayerInRoom(player, room);
    }
}
