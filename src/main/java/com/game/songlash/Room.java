package com.game.songlash;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    private final String code;
    private final List<String> sessionIds = new CopyOnWriteArrayList<>();
    private final String hostSessionId; // Added field

    //constructors
    public Room(String code, String hostSessionId) { // Modified constructor
        this.code = code;
        this.hostSessionId = hostSessionId;
    }

    //getters
    public String getCode() {return code;}
    public List<String> getSessionIds() {return sessionIds;}
    public String getHostSessionId() {return hostSessionId;}

    //check if session is the host
    public boolean isHostSession(String sessionId) {
        return hostSessionId != null && hostSessionId.equals(sessionId);
    }

    //setters
    //no
}