package com.example.goprox;

public class ChatSummary {
    private String otherUserId;
    private String receiverName;
    private String lastMessage;
    private long timestamp;
    private int unreadCount;

    public ChatSummary() {}

    public ChatSummary(String otherUserId, String receiverName, String lastMessage, long timestamp, int unreadCount) {
        this.otherUserId = otherUserId;
        this.receiverName = receiverName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
    }

    // Геттеры и сеттеры (оставьте как было)
    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}