package com.example.goprox;

public class ChatMessage {
    private String id;
    private String senderId;
    private String text;
    private String type;
    private String fileUrl;
    private long timestamp;
    private int voiceDuration;
    private boolean isRead;
    private boolean isUploading;
    private int uploadProgress;
    private String fileSize;

    public ChatMessage() {}

    public ChatMessage(String id, String senderId, String text, String type, String fileUrl, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
        this.voiceDuration = 0;
        this.isRead = false;
        this.isUploading = false;
        this.uploadProgress = 0;
        this.fileSize = null;
    }

    // Getters & Setters
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
    public int getVoiceDuration() { return voiceDuration; }
    public void setVoiceDuration(int voiceDuration) { this.voiceDuration = voiceDuration; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public boolean isUploading() { return isUploading; }
    public void setUploading(boolean uploading) { isUploading = uploading; }
    public int getUploadProgress() { return uploadProgress; }
    public void setUploadProgress(int uploadProgress) { this.uploadProgress = uploadProgress; }
    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }
}