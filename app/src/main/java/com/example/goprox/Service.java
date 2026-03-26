package com.example.goprox;

public class Service {
    private String name;
    private String profession;
    private String description;
    private String price;
    private float rating;
    private int imageResId;
    private String userId;

    // Constructor
    public Service(String name, String profession, String description, String price, float rating, int imageResId, String userId) {
        this.name = name;
        this.profession = profession;
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.imageResId = imageResId;
        this.userId = userId;
    }

    // Getters
    public String getName() { return name; }
    public String getProfession() { return profession; }
    public String getDescription() { return description; }
    public String getPrice() { return price; }
    public float getRating() { return rating; }
    public int getImageResId() { return imageResId; }
    public String getUserId() { return userId; }
}