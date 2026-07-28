package com.game.songlash;

import java.util.List;

public class ChatMessage {
    private String content;
    private String sender;
    private MessageType type;
    private List<String> players;

    //constructors
    public ChatMessage(){}

    public ChatMessage(String content, String sender, MessageType type, List<String> players) {
        this.content = content;
        this.sender = sender;
        this.type = type;
        this.players = players;
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

        public ChatMessage build(){
            return new ChatMessage(content, sender, type, players);
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

    //setters
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
}
