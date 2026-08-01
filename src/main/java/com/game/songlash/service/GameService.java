package com.game.songlash.service;

import com.game.songlash.exception.GameException;
import com.game.songlash.model.Room;
import com.game.songlash.repository.PlayerRegistry;
import com.game.songlash.repository.RoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final RoomManager roomManager;
    private final PlayerRegistry playerRegistry;

    public Room createRoom(String sessionId) {
        leaveCurrentRoom(sessionId);

        Room room = roomManager.createRoom(sessionId);
        playerRegistry.ensureSession(sessionId);
        playerRegistry.setPlayerRoom(sessionId, room.getCode());
        room.tryAddSession(sessionId); // a brand new room always has space

        return room;
    }

    public Room joinRoom(String sessionId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();

        Room room = roomManager.getRoom(code);
        if (room == null) {
            throw new GameException("room doesn't exist");
        }

        leaveCurrentRoom(sessionId);

        if (!room.tryAddSession(sessionId)) {
            throw new GameException("room is full");
        }

        playerRegistry.ensureSession(sessionId);
        playerRegistry.setPlayerRoom(sessionId, code);

        return room;
    }

    private void leaveCurrentRoom(String sessionId) {
        playerRegistry.find(sessionId)
                .flatMap(player -> player.roomId())
                .ifPresent(code -> {
                    Room previous = roomManager.getRoom(code);
                    if (previous == null) return;

                    previous.removeSession(sessionId);
                    if (previous.isEmpty()) {
                        roomManager.removeRoom(code);
                        log.info("room {} removed (empty)", code);
                    }
                });
    }
}
