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
                            String name = document.getString("name");
                            String profession = document.getString("profession");
                            String description = document.getString("description");
                            String price = document.getString("price");
                            Double rating = document.getDouble("rating");
                            String userId = document.getString("userId");
                            services.add(new Service(
                                    name != null ? name : "Unknown",
                                    profession != null ? profession : "Unknown",
                                    description != null ? description : "",
                                    price != null ? price : "$0",
                                    rating != null ? rating.floatValue() : 0,
                                    R.drawable.ic_profile_placeholder,
                                    userId != null ? userId : ""
                            ));
                        }
                    }
                    callback.onCallback(services);
                });
    }
}