package com.example.goprox;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirebaseService {
    private final FirebaseFirestore db;

    public interface ServiceCallback {
        void onCallback(List<Service> services);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    public void getAllServices(ServiceCallback callback) {
        if (callback == null) return;

        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    List<Service> services = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Service service = parseQueryDocument(document);
                            if (service != null) {
                                services.add(service);
                            }
                        }
                    }
                    callback.onCallback(services);
                })
                .addOnFailureListener(e -> {
                    callback.onCallback(new ArrayList<>());
                });
    }

    public void getAllServicesWithTags(ServiceCallback callback) {
        getAllServices(callback);
    }

    public void getServiceById(String serviceId, ServiceCallback callback) {
        if (callback == null || serviceId == null) return;

        db.collection("services").document(serviceId)
                .get()
                .addOnSuccessListener(document -> {
                    List<Service> services = new ArrayList<>();
                    if (document != null && document.exists()) {
                        Service service = parseDocumentSnapshot(document);
                        if (service != null) {
                            services.add(service);
                        }
                    }
                    callback.onCallback(services);
                })
                .addOnFailureListener(e -> {
                    callback.onCallback(new ArrayList<>());
                });
    }

    public void deleteService(String serviceId, OnDeleteListener listener) {
        if (serviceId == null) {
            if (listener != null) listener.onFailure("Service ID is null");
            return;
        }

        db.collection("services").document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage() != null ? e.getMessage() : "Delete failed");
                    }
                });
    }

    public void deleteAllServices(OnDeleteListener listener) {
        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int size = task.getResult().size();
                        if (size == 0) {
                            if (listener != null) listener.onSuccess();
                            return;
                        }
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            doc.getReference().delete();
                        }
                        if (listener != null) listener.onSuccess();
                    } else {
                        if (listener != null) {
                            String error = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Failed to get services";
                            listener.onFailure(error);
                        }
                    }
                });
    }

    public void deleteMyServices(OnDeleteListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (listener != null) listener.onFailure("User not signed in");
            return;
        }

        String currentUserId = currentUser.getUid();
        db.collection("services")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int size = task.getResult().size();
                        if (size == 0) {
                            if (listener != null) listener.onSuccess();
                            return;
                        }
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            doc.getReference().delete();
                        }
                        if (listener != null) listener.onSuccess();
                    } else {
                        if (listener != null) {
                            String error = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Failed to get services";
                            listener.onFailure(error);
                        }
                    }
                });
    }

    // For QueryDocumentSnapshot (from collection queries)
    private Service parseQueryDocument(QueryDocumentSnapshot doc) {
        if (doc == null) return null;
        return parseData(doc.getId(), doc);
    }

    // For DocumentSnapshot (from single document get)
    private Service parseDocumentSnapshot(DocumentSnapshot doc) {
        if (doc == null) return null;
        return parseData(doc.getId(), doc);
    }

    // Common parser
    private Service parseData(String serviceId, DocumentSnapshot document) {
        if (document == null) return null;

        try {
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
            String country = document.getString("country");
            String city = document.getString("city");
            Double latitude = document.getDouble("latitude");
            Double longitude = document.getDouble("longitude");

            if (tags == null) tags = new ArrayList<>();
            int ratingCount = ratingCountLong != null ? ratingCountLong.intValue() : 0;
            float ratingValue = rating != null ? rating.floatValue() : 0f;

            return new Service(
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
            );
        } catch (Exception e) {
            return null;
        }
    }
}