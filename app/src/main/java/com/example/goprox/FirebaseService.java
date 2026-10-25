
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
        if (callback == null) {
            return;
        }

        db.collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Service> services = new ArrayList<>();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            Service service = parseQueryDocument(document);

                            if (service != null) {
                                services.add(service);
                            }
                        }
                    }

                    callback.onCallback(services);
                })
                .addOnFailureListener(error ->
                        callback.onCallback(new ArrayList<>())
                );
    }

    public void getAllServicesWithTags(ServiceCallback callback) {
        getAllServices(callback);
    }

    public void getServiceById(String serviceId, ServiceCallback callback) {
        if (callback == null || serviceId == null
                || serviceId.trim().isEmpty()) {
            if (callback != null) {
                callback.onCallback(new ArrayList<>());
            }
            return;
        }

        db.collection("services")
                .document(serviceId)
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
                .addOnFailureListener(error ->
                        callback.onCallback(new ArrayList<>())
                );
    }

    public void deleteService(String serviceId, OnDeleteListener listener) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            if (listener != null) {
                listener.onFailure("Service ID is missing");
            }
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            if (listener != null) {
                listener.onFailure("User not signed in");
            }
            return;
        }

        db.collection("services")
                .document(serviceId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document == null || !document.exists()) {
                        if (listener != null) {
                            listener.onFailure("Service not found");
                        }
                        return;
                    }

                    String ownerId = document.getString("userId");

                    if (ownerId == null
                            || !ownerId.equals(currentUser.getUid())) {
                        if (listener != null) {
                            listener.onFailure(
                                    "You can delete only your own service"
                            );
                        }
                        return;
                    }

                    document.getReference()
                            .delete()
                            .addOnSuccessListener(unused -> {
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(error -> {
                                if (listener != null) {
                                    listener.onFailure(getErrorMessage(
                                            error,
                                            "Delete failed"
                                    ));
                                }
                            });
                })
                .addOnFailureListener(error -> {
                    if (listener != null) {
                        listener.onFailure(getErrorMessage(
                                error,
                                "Could not verify service ownership"
                        ));
                    }
                });
    }

    /**
     * Deletes only services owned by the currently signed-in user.
     */
    public void deleteMyServices(OnDeleteListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            if (listener != null) {
                listener.onFailure("User not signed in");
            }
            return;
        }

        db.collection("services")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        if (listener != null) {
                            listener.onSuccess();
                        }
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot document : snapshot) {
                        batch.delete(document.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(error -> {
                                if (listener != null) {
                                    listener.onFailure(getErrorMessage(
                                            error,
                                            "Failed to delete your services"
                                    ));
                                }
                            });
                })
                .addOnFailureListener(error -> {
                    if (listener != null) {
                        listener.onFailure(getErrorMessage(
                                error,
                                "Failed to load your services"
                        ));
                    }
                });
    }

    /**
     * Bulk deletion of every user's services is intentionally disabled.
     */
    public void deleteAllServices(OnDeleteListener listener) {
        if (listener != null) {
            listener.onFailure(
                    "Deleting every user's services is disabled for safety. "
                            + "Use deleteMyServices() instead."
            );
        }
    }

    private Service parseQueryDocument(QueryDocumentSnapshot document) {
        if (document == null) {
            return null;
        }

        return parseData(document.getId(), document);
    }

    private Service parseDocumentSnapshot(DocumentSnapshot document) {
        if (document == null) {
            return null;
        }

        return parseData(document.getId(), document);
    }

    private Service parseData(String serviceId, DocumentSnapshot document) {
        if (document == null) {
            return null;
        }

        try {
            String name = document.getString("name");
            String profession = document.getString("profession");
            String description = document.getString("description");

            // Supports numeric prices and legacy string prices.
            Object rawPrice = document.get("price");
            String price = ServicePriceParser.toPriceString(rawPrice);

            String priceType = document.getString("priceType");

            Double rating = readDouble(document, "rating");
            Long ratingCountLong = readLong(document, "ratingCount");

            String userId = document.getString("userId");

            List<String> tags = readStringList(document.get("tags"));

            String imageUrl = document.getString("imageUrl");
            String country = document.getString("country");
            String city = document.getString("city");

            Double latitude = readDouble(document, "latitude");
            Double longitude = readDouble(document, "longitude");

            int ratingCount = ratingCountLong != null
                    ? Math.max(0, ratingCountLong.intValue())
                    : 0;

            float ratingValue = rating != null
                    ? rating.floatValue()
                    : 0f;

            ratingValue = Math.max(0f, Math.min(5f, ratingValue));

            Service service = new Service(
                    serviceId,
                    name != null ? name : "Unknown",
                    profession != null ? profession : "Unknown",
                    description != null ? description : "",
                    price,
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

            if (priceType != null && !priceType.trim().isEmpty()) {
                service.setPriceType(priceType);
            }

            return service;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double readDouble(DocumentSnapshot document, String field) {
        Object value = document.get(field);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private Long readLong(DocumentSnapshot document, String field) {
        Object value = document.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private List<String> readStringList(Object rawTags) {
        List<String> result = new ArrayList<>();

        if (!(rawTags instanceof List<?>)) {
            return result;
        }

        for (Object item : (List<?>) rawTags) {
            if (item instanceof String) {
                String tag = ((String) item).trim();

                if (!tag.isEmpty()) {
                    result.add(tag);
                }
            }
        }

        return result;
    }

    private String getErrorMessage(Exception error, String fallback) {
        if (error == null || error.getMessage() == null
                || error.getMessage().trim().isEmpty()) {
            return fallback;
        }

        return error.getMessage();
    }
}
