package com.example.goprox;

import java.util.List;

public class Service {
    private String id;
    private String name;
    private String profession;
    private String description;
    private String price;
    private float rating;
    private int ratingCount;
    private String imageUrl;
    private String userId;
    private List<String> tags;          // ⬅️ новое поле

    public Service() {
        // Пустой конструктор для Firestore
    }

    public Service(String id, String name, String profession, String description,
                   String price, float rating, int ratingCount, String imageUrl,
                   String userId, List<String> tags) {
        this.id = id;
        this.name = name;
        this.profession = profession;
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}