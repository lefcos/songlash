package com.game.songlash;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public Room createRoom(){
        String code = generateUniqueRoomCode();
        Room newRoom = new Room(code);
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
}
