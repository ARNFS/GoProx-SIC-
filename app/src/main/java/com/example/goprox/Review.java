package com.example.goprox;

public class Review {
    private String userName;
    private float rating;
    private String comment;
    private long timestamp;

    public Review() {}

    public Review(String userName, float rating, String comment, long timestamp) {
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getUserName() { return userName; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public long getTimestamp() { return timestamp; }
}