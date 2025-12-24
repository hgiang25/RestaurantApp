package com.example.restaurantapp.models;

/**
 * Model cho tin nhắn trong chatbot
 */
public class ChatMessage {
    
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    
    private String id;
    private String message;
    private int type; // TYPE_USER or TYPE_BOT
    private long timestamp;
    private boolean isTyping; // Hiển thị đang nhập

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String message, int type) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.isTyping = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }
    
    public boolean isUserMessage() {
        return type == TYPE_USER;
    }
    
    public boolean isBotMessage() {
        return type == TYPE_BOT;
    }
}
