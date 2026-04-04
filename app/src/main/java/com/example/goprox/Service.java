package com.example.goprox;

import java.util.List;

public class Service {
    private String serviceId;
    private String name;
    private String profession;
    private String description;
    private String price;
    private float rating;
    private int ratingCount;
    private int imageResId;
    private String imageUrl;
    private String userId;
    private List<String> tags;

    public Service(String serviceId, String name, String profession, String description,
                   String price, float rating, int ratingCount, int imageResId,
                   String userId, List<String> tags, String imageUrl) {
        this.serviceId = serviceId;
        this.name = name;
        this.profession = profession;
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.imageResId = imageResId;
        this.userId = userId;
        this.tags = tags;
        this.imageUrl = imageUrl;
    }

    public Service(String name, String profession, String description,
                   String price, float rating, int ratingCount, int imageResId,
                   String userId, List<String> tags, String imageUrl) {
        this(null, name, profession, description, price, rating, ratingCount, imageResId, userId, tags, imageUrl);
    }

    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public String getProfession() { return profession; }
    public String getDescription() { return description; }
    public String getPrice() { return price; }
    public float getRating() { return rating; }
    public int getRatingCount() { return ratingCount; }
    public int getImageResId() { return imageResId; }
    public String getImageUrl() { return imageUrl; }
    public String getUserId() { return userId; }
    public List<String> getTags() { return tags; }

    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public void setRating(float rating) { this.rating = rating; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}