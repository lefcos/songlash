package com.game.songlash.model;

import java.util.List;

public class GameMessage {
    private String content;
    private String sender;
    private MessageType type;
    private List<String> players;
    private Boolean host; // Added field
    private String hostName; // Added field

    //constructors
    public GameMessage(){}

    public GameMessage(String content, String sender, MessageType type, List<String> players, Boolean host, String hostName) { // Modified constructor
        this.content = content;
        this.sender = sender;
        this.type = type;
        this.players = players;
        this.host = host;
        this.hostName = hostName;
    }

    //builders
    public static ChatMessageBuilder builder(){
        return new ChatMessageBuilder();
    }

    public static class ChatMessageBuilder{
        private String content;
        private String sender;
        private MessageType type;
        private List<String> players;
        private Boolean host;
        private String hostName;

        private ChatMessageBuilder(){};

        public ChatMessageBuilder content(String content){
            this.content = content;
            return this;
        }

        public ChatMessageBuilder sender(String sender){
            this.sender = sender;
            return this;
        }

        public ChatMessageBuilder type(MessageType type){
            this.type = type;
            return this;
        }

        public ChatMessageBuilder players(List<String> players){
            this.players = players;
            return this;
        }

        public ChatMessageBuilder host(Boolean host){
            this.host = host;
            return this;
        }

        public ChatMessageBuilder hostName(String hostName){
            this.hostName = hostName;
            return this;
        }

        public GameMessage build(){
            return new GameMessage(content, sender, type, players, host, hostName);
        }
    }

    //getters
    public String getContent(){
        return content;
    }

    public String getSender(){
        return sender;
    }

    public MessageType getType(){
        return type;
    }

    public List<String> getPlayers(){
        return players;
    }

    public Boolean getHost() { // Added getter
        return host;
    }

    public String getHostName() { // Added getter
        return hostName;
    }

    //setters
    //keep these IDK WHY
    public void setContent(String content){
        this.content = content;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public void setType(MessageType type){
        this.type = type;
    }

    public void setPlayers(List<String> players){
        this.players = players;
    }

    public void setHost(Boolean host) { // Added setter
        this.host = host;
    }

    public void setHostName(String hostName) { // Added setter
        this.hostName = hostName;
    }
}