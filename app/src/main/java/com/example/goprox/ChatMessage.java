package com.example.goprox;

public class ChatMessage {
    private String id;
    private String senderId;
    private String text;        // для текста – сам текст, для файлов – имя файла
    private String type;        // "text", "image", "file", "voice"
    private String fileUrl;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String id, String senderId, String text, String type, String fileUrl, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры (оставьте как было)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}