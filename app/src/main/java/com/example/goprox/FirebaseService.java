package com.example.goprox;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirebaseService {
    private FirebaseFirestore db;

    public interface ServiceCallback {
        void onCallback(List<Service> services);
    }

    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    public void getAllServices(ServiceCallback callback) {
        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    List<Service> services = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String serviceId = document.getId();
                            String name = document.getString("name");
                            String profession = document.getString("profession");
                            String description = document.getString("description");
                            String price = document.getString("price");
                            Double rating = document.getDouble("rating");
                            Long ratingCountLong = document.getLong("ratingCount");
                            String userId = document.getString("userId");
                            @SuppressWarnings("unchecked")
                            List<String> tags = (List<String>) document.get("tags");
                            String imageUrl = document.getString("imageUrl");

                            // Всегда читаем country и city
                            String country = document.getString("country");
                            String city = document.getString("city");

                            // Координаты (могут быть null)
                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");

                            if (tags == null) tags = new ArrayList<>();
                            int ratingCount = ratingCountLong != null ? ratingCountLong.intValue() : 0;
                            float ratingValue = rating != null ? rating.floatValue() : 0f;

                            // Всегда используем новый конструктор (с country и city)
                            services.add(new Service(
                                    serviceId,
                                    name != null ? name : "Unknown",
                                    profession != null ? profession : "Unknown",
                                    description != null ? description : "",
                                    price != null ? price : "$0",
                                    ratingValue,
                                    ratingCount,
                                    imageUrl,
                                    userId != null ? userId : "",
                                    tags,
                                    latitude != null ? latitude : 0.0,
                                    longitude != null ? longitude : 0.0,
                                    country,
                                    city
                            ));
                        }
                    }
                    callback.onCallback(services);
                });
    }

    public void getAllServicesWithTags(ServiceCallback callback) {
        getAllServices(callback);
    }

    public void getServiceById(String serviceId, ServiceCallback callback) {
        db.collection("services").document(serviceId)
                .get()
                .addOnSuccessListener(document -> {
                    List<Service> services = new ArrayList<>();
                    if (document.exists()) {
                        String name = document.getString("name");
                        String profession = document.getString("profession");
                        String description = document.getString("description");
                        String price = document.getString("price");
                        Double rating = document.getDouble("rating");
                        Long ratingCountLong = document.getLong("ratingCount");
                        String userId = document.getString("userId");
                        @SuppressWarnings("unchecked")
                        List<String> tags = (List<String>) document.get("tags");
                        String imageUrl = document.getString("imageUrl");
                        Double latitude = document.getDouble("latitude");
                        Double longitude = document.getDouble("longitude");
                        String country = document.getString("country");
                        String city = document.getString("city");

                        if (tags == null) tags = new ArrayList<>();
                        int ratingCount = ratingCountLong != null ? ratingCountLong.intValue() : 0;
                        float ratingValue = rating != null ? rating.floatValue() : 0f;

                        if (latitude == null || longitude == null) {
                            services.add(new Service(
                                    serviceId,
                                    name != null ? name : "Unknown",
                                    profession != null ? profession : "Unknown",
                                    description != null ? description : "",
                                    price != null ? price : "$0",
                                    ratingValue,
                                    ratingCount,
                                    imageUrl,
                                    userId != null ? userId : "",
                                    tags
                            ));
                        } else {
                            services.add(new Service(
                                    serviceId,
                                    name != null ? name : "Unknown",
                                    profession != null ? profession : "Unknown",
                                    description != null ? description : "",
                                    price != null ? price : "$0",
                                    ratingValue,
                                    ratingCount,
                                    imageUrl,
                                    userId != null ? userId : "",
                                    tags,
                                    latitude,
                                    longitude,
                                    country,
                                    city
                            ));
                        }
                    }
                    callback.onCallback(services);
                });
    }

    public void deleteService(String serviceId, OnDeleteListener listener) {
        db.collection("services").document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    public void deleteAllServices(OnDeleteListener listener) {
        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            doc.getReference().delete();
                        }
                        if (listener != null) listener.onSuccess();
                    } else {
                        if (listener != null) listener.onFailure("Failed to get services");
                    }
                });
    }

    public void deleteMyServices(OnDeleteListener listener) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("services")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            doc.getReference().delete();
                        }
                        if (listener != null) listener.onSuccess();
                    } else {
                        if (listener != null) listener.onFailure("Failed to get services");
                    }
                });
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}