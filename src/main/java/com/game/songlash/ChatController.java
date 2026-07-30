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

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/room", messageCreated, accessor.getMessageHeaders());
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

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/room", message, accessor.getMessageHeaders());
    }

    @MessageMapping("/chat.setName")
    public void setName(@Payload ChatMessage in, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();
        String requested = in.getSender() == null ? "" : in.getSender().trim();
        //TODO: name restrictions

        Optional<Player> maybePlayer = playerRegistry.find(sessionId);
        if (maybePlayer.isEmpty() || maybePlayer.get().roomId().isEmpty()) {
            sendError(sessionId, "join a room first");
            return;
        }

        Player player = maybePlayer.get();
        String roomCode = player.roomId().get();
        Room room = roomManager.getRoom(roomCode);

        if (playerRegistry.isNameTaken(roomCode, requested)) {
            sendError(sessionId, "name is taken in this room");
            return;
        }

        playerRegistry.setPlayerName(sessionId, requested);

        String hostName = playerRegistry.find(room.getHostSessionId())
                .flatMap(Player::name)
                .orElse("Host");

        messagingTemplate.convertAndSend("/topic/room." + roomCode, ChatMessage.builder()
                .type(MessageType.JOIN)
                .sender(requested)
                .content(requested + " joined the room")
                .players(playerRegistry.namesInRoom(roomCode))
                .hostName(hostName)
                .build());
    }

    @MessageMapping("/chat.startGame")
    public void startGame(SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();

        Optional<Player> maybePlayer = playerRegistry.find(sessionId);
        if (maybePlayer.isEmpty() || maybePlayer.get().roomId().isEmpty()) {
            sendError(sessionId, "join a room first");
            return;
        }

        Player player = maybePlayer.get();
        String roomCode = player.roomId().get();
        Room room = roomManager.getRoom(roomCode);

        if (room == null || !room.isHostSession(sessionId)) {
            sendError(sessionId, "only the host can start the game");
            return;
        }

        messagingTemplate.convertAndSend("/topic/room." + roomCode, ChatMessage.builder()
                .type(MessageType.START_GAME)
                .sender("system")
                .content("game started")
                .build());
    }

    @MessageMapping("/chat.roomMessage")
    public void roomMessage(@Payload ChatMessage in, SimpMessageHeaderAccessor accessor){
        String sessionId = accessor.getSessionId();

        Optional<Player> maybePlayer = playerRegistry.find(sessionId);
        if (maybePlayer.isEmpty() || maybePlayer.get().roomId().isEmpty()) {
            sendError(sessionId, "join a room first");
            return;
        }

        Player player = maybePlayer.get();
        if (player.name().isEmpty()) {
            sendError(sessionId, "set your name first");
            return;
        }

        String content = in.getContent() == null ? "" : in.getContent().trim();
        if(content.isEmpty() || content.length() > 200){
            return;
        }

        messagingTemplate.convertAndSend("/topic/room." + player.roomId().get(), ChatMessage.builder()
                .type(MessageType.CHAT)
                .sender(player.name().get())
                .content(content)
                .build());
    }
}