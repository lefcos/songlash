package com.game.songlash.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    public enum State { LOBBY, ONGOING, DONE }

    public static final int MAX_PLAYERS = 7;

    private final String code;
    private final List<String> sessionIds = new CopyOnWriteArrayList<>();
    private final String hostSessionId; // Added field
    private State currentState;
    private GameSession gameSession;

    //constructors
    public Room(String code, String hostSessionId) { // Modified constructor
        this.code = code;
        this.hostSessionId = hostSessionId;
        this.currentState = State.LOBBY;
        this.gameSession = null;
    }

    //getters
    public String getCode() {return code;}
    public List<String> getSessionIds() {return List.copyOf(sessionIds);}
    public String getHostSessionId() {return hostSessionId;}
    public State getCurrentState() { return currentState; }
    public GameSession getGameSession() { return gameSession; }

    public int playerCount() { return sessionIds.size(); }
    public boolean isEmpty() { return sessionIds.isEmpty(); }

    //check if session is the host
    public boolean isHostSession(String sessionId) {
        return hostSessionId != null && hostSessionId.equals(sessionId);
    }

    public synchronized boolean tryAddSession(String sessionId) {
        if (sessionIds.contains(sessionId)) return true;
        if (sessionIds.size() >= MAX_PLAYERS) return false;
        sessionIds.add(sessionId);
        return true;
    }

    public synchronized void removeSession(String sessionId) {
        sessionIds.remove(sessionId);
    }

    //setters
    public void startGame() {
        if (this.currentState == State.LOBBY) {
            this.currentState = State.ONGOING;
            this.gameSession = new GameSession();
        }
    }
}
