package com.game.songlash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j

public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRegistry playerRegistry;
    private final RoomManager roomManager;
    private record PlayerInRoom(Player player, Room room) {}

    @MessageMapping("/chat.createRoom")
    public void createRoom(SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        Room room = roomManager.createRoom(sessionId);
        playerRegistry.ensureSession(sessionId);
        playerRegistry.setPlayerRoom(sessionId, room.getCode());
        room.getSessionIds().add(sessionId);

        String hostName = playerRegistry.find(sessionId)
                .flatMap(Player::name)
                .orElse("Host");

        ChatMessage messageCreated = ChatMessage.builder()
                .type(MessageType.ROOM_CREATED)
                .sender("system")
                .content("room created with code " + room.getCode())
                .players(playerRegistry.namesInRoom(room.getCode()))
                .host(true)
                .hostName(hostName)
                .build();
        sendToUser(sessionId, "/queue/room", messageCreated);
    }

    @MessageMapping("/chat.joinRoom")
    public void joinRoom(@Payload ChatMessage in, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();
        String code = in.getContent() == null ? "" : in.getContent().trim().toUpperCase();

        Room room = roomManager.getRoom(code);
        if(room == null){
            sendError(sessionId, "room doesn't exist");
            return;
        }

        if(roomManager.isFull(room)){
            sendError(sessionId, "room is full");
            return;
        }

        playerRegistry.ensureSession(sessionId);
        playerRegistry.setPlayerRoom(sessionId, code);
        room.getSessionIds().add(sessionId);

        boolean isHost = room.isHostSession(sessionId);
        String hostName = playerRegistry.find(room.getHostSessionId())
                .flatMap(Player::name)
                .orElse("Host");

        ChatMessage message = ChatMessage.builder()
                .type(MessageType.ROOM_JOINED)
                .sender("system")
                .content("joined room with code " + code)
                .players(playerRegistry.namesInRoom(code))
                .host(isHost)
                .hostName(hostName)
                .build();
        sendToUser(sessionId, "/queue/room", message);
    }

    @MessageMapping("/chat.setName")
    public void setName(@Payload ChatMessage in, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();
        String requested = in.getSender() == null ? "" : in.getSender().trim();

        if(requested.length() >= 12 || requested.length() < 2){
            sendError(sessionId, "name must be between 2 and 12 characters");
            return;
        }

        Optional<PlayerInRoom> context = getPlayerAndRoom(sessionId);
        if (context.isEmpty()) return;

        if (playerRegistry.isNameTaken(context.get().room().getCode(), requested)) {
            sendError(sessionId, "name is taken in this room");
            return;
        }

        playerRegistry.setPlayerName(sessionId, requested);

        String hostName = playerRegistry.find(context.get().room().getHostSessionId())
                .flatMap(Player::name)
                .orElse("Host");

        sendToRoom(context.get().room().getCode(), ChatMessage.builder()
                .type(MessageType.JOIN)
                .sender(requested)
                .content(requested + " joined the room")
                .players(playerRegistry.namesInRoom(context.get().room().getCode()))
                .hostName(hostName)
                .build());
    }

    @MessageMapping("/chat.startGame")
    public void startGame(SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        Optional<PlayerInRoom> context = getPlayerAndRoom(sessionId);
        if (context.isEmpty()) return;

        if (!context.get().room().isHostSession(sessionId)) {
            sendError(sessionId, "only the host can start the game");
            return;
        }

        // TODO: game state logic
        sendToRoom(context.get().room().getCode(), ChatMessage.builder()
                .type(MessageType.START_GAME)
                .sender("system")
                .content("game started")
                .build());
    }

    @MessageMapping("/chat.roomMessage")
    public void roomMessage(@Payload ChatMessage in, SimpMessageHeaderAccessor accessor){
        String sessionId = accessor.getSessionId();

        Optional<PlayerInRoom> context = getPlayerAndRoom(sessionId);
        if (context.isEmpty()) return;

        if (context.get().player().name().isEmpty()) {
            sendError(sessionId, "set your name first");
            return;
        }

        String content = in.getContent() == null ? "" : in.getContent().trim();
        if(content.isEmpty() || content.length() > 200){
            return;
        }

        sendToRoom(context.get().room().getCode(), ChatMessage.builder()
                .type(MessageType.CHAT)
                .sender(context.get().player().name().get())
                .content(content)
                .build());
    }

    //helper funcs
    private void sendError(String sessionId, String text){
        var error = ChatMessage.builder()
                .type(MessageType.ERROR)
                .content(text)
                .build();
        sendToUser(sessionId, "/queue/errors", error);
    }
    private void sendToUser(String sessionId, String destination, Object payload) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload, accessor.getMessageHeaders());
    }
    private void sendToRoom(String roomCode, Object payload) {
        messagingTemplate.convertAndSend("/topic/room." + roomCode, payload);
    }
    private Optional<PlayerInRoom> getPlayerAndRoom(String sessionId) {
        Optional<Player> maybePlayer = playerRegistry.find(sessionId);
        if (maybePlayer.isEmpty() || maybePlayer.get().roomId().isEmpty()) {
            sendError(sessionId, "join a room first");
            return Optional.empty();
        }

        Player player = maybePlayer.get();
        Room room = roomManager.getRoom(player.roomId().get());
        if (room == null) {
            sendError(sessionId, "room does not exist");
            return Optional.empty();
        }
        return Optional.of(new PlayerInRoom(player, room));
    }
}