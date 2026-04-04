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

                            if (tags == null) tags = new ArrayList<>();
                            int ratingCount = ratingCountLong != null ? ratingCountLong.intValue() : 0;
                            float ratingValue = rating != null ? rating.floatValue() : 0f;

                            services.add(new Service(
                                    serviceId,
                                    name != null ? name : "Unknown",
                                    profession != null ? profession : "Unknown",
                                    description != null ? description : "",
                                    price != null ? price : "$0",
                                    ratingValue,
                                    ratingCount,
                                    R.drawable.ic_profile_placeholder,
                                    userId != null ? userId : "",
                                    tags,
                                    imageUrl
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

                        if (tags == null) tags = new ArrayList<>();
                        int ratingCount = ratingCountLong != null ? ratingCountLong.intValue() : 0;
                        float ratingValue = rating != null ? rating.floatValue() : 0f;

                        services.add(new Service(
                                serviceId,
                                name != null ? name : "Unknown",
                                profession != null ? profession : "Unknown",
                                description != null ? description : "",
                                price != null ? price : "$0",
                                ratingValue,
                                ratingCount,
                                R.drawable.ic_profile_placeholder,
                                userId != null ? userId : "",
                                tags,
                                imageUrl
                        ));
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