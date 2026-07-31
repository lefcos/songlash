package com.game.songlash.repository;

import com.game.songlash.model.Room;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component //add this to tell Spring that this file is a component (?)

public class RoomManager {
    private final Map<String, Room> activeRooms = new ConcurrentHashMap<>();
    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    private String generateUniqueRoomCode(){
        String code;

        do{
            StringBuilder sb = new StringBuilder(4);
            for(int i=0; i < 4; i++){
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }

            code = sb.toString();
        }while(activeRooms.containsKey(code));

        return code;
    }

    public Room createRoom(String hostSessionId){
        String code = generateUniqueRoomCode();
        Room newRoom = new Room(code, hostSessionId);
        activeRooms.put(code, newRoom);
        System.out.println("new room made with code " + code);
        return newRoom;
    }

    public Room getRoom(String code){
        return activeRooms.get(code);
    }

    public void removeRoom(String code){
        activeRooms.remove(code);
    }

    public boolean isFull(Room room){
        return room.getSessionIds().size() >= 7;
    }
}