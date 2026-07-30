package com.game.songlash;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    private final String code;
    private final List<String> sessionIds = new CopyOnWriteArrayList<>();
    //copyonwritearraylist = multiple players will be added/removed from threads at the same time, safer than ArrayList

    //constructors
    public Room(String code) {
        this.code = code;
    }

    //getters
    public String getCode() {return code;}
    public List<String> getSessionIds() {return sessionIds;}

    //setters
    //no
}