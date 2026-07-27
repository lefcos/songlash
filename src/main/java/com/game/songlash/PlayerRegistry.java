package com.game.songlash;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class PlayerRegistry {
    private static final Map<String, Player> bySession = new ConcurrentHashMap<>();

    public synchronized Optional<Player> add(String sessionId, String name){
        boolean taken = bySession.values().stream().anyMatch(p->p.name().equalsIgnoreCase(name));

        if(taken){
            return Optional.empty();
        }

        Player player = new Player(sessionId, name);
        bySession.put(sessionId, player);
        return Optional.of(player);
    }

    public Optional<Player> remove(String sessionId){
        return Optional.ofNullable(bySession.remove(sessionId));
    }

    public Optional<Player> find(String sessionId){
        return Optional.ofNullable(bySession.get(sessionId));
    }

    public List<String> names(){
        return bySession.values().stream()
                .map(Player::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

}
