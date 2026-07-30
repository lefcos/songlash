package com.game.songlash;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class PlayerRegistry {
    private static final Map<String, Player> bySession = new ConcurrentHashMap<>();

    public Player ensureSession(String sessionId){
        return bySession.computeIfAbsent(sessionId,
                id -> new Player(id, Optional.empty(), Optional.empty())
        );
    }

    public Optional<Player> remove(String sessionId){
        return Optional.ofNullable(bySession.remove(sessionId));
    }

    public Optional<Player> find(String sessionId){
        return Optional.ofNullable(bySession.get(sessionId));
    }

    public boolean setPlayerRoom(String sessionId, String roomId){
        Player existing = ensureSession(sessionId);
        bySession.put(sessionId, new Player(sessionId, existing.name(), Optional.of(roomId)));
        return true;
    }

    public boolean setPlayerName(String sessionId, String name){
        Optional<Player> maybe = find(sessionId);
        if (maybe.isEmpty()) return false;

        Player p = maybe.get();
        bySession.put(sessionId, new Player(sessionId, Optional.of(name), p.roomId()));
        return true;
    }

    public boolean isNameTaken(String roomCode, String requestedName){
        return bySession.values().stream().anyMatch(p ->
                p.roomId().isPresent()
                        && p.roomId().get().equals(roomCode)
                        && p.name().isPresent()
                        && p.name().get().equalsIgnoreCase(requestedName));
    }

    public List<String> namesInRoom(String roomCode){
        return bySession.values().stream()
                .filter(p -> p.roomId().isPresent() && p.roomId().get().equals(roomCode))
                .flatMap(p -> p.name().stream())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}