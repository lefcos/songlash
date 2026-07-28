package com.game.songlash;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class PlayerRegistry {
    private static final Map<String, Player> bySession = new ConcurrentHashMap<>();

    public synchronized Optional<Player> add(String sessionId, String name, Optional<String> roomId){
        boolean taken = bySession.values().stream().anyMatch(p->p.name().equalsIgnoreCase(name));

        if(taken){
            return Optional.empty();
        }

        Player player = new Player(sessionId, name, roomId);
        bySession.put(sessionId, player);
        return Optional.of(player);
    }

    public Optional<Player> remove(String sessionId){
        return Optional.ofNullable(bySession.remove(sessionId));
    }

    public Optional<Player> find(String sessionId){
        return Optional.ofNullable(bySession.get(sessionId));
    }

    public boolean setPlayerRoom(String sessionId, String roomId){
        Optional<Player> maybePlayer = find(sessionId);
        if(maybePlayer.isEmpty()) return false;

        Player oldPlayer = maybePlayer.get();
        Player updatedPlayer = new Player(sessionId, oldPlayer.name(), Optional.of(roomId));
        bySession.put(sessionId, updatedPlayer);
        return true;
    }

    public List<String> names(){
        return bySession.values().stream()
                .map(Player::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}