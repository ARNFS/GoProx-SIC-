package com.example.goprox;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Fills missing identity fields in both profile stores without replacing profile data. */
final class AuthProfileSync {
    private static final String DATABASE_URL =
            "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app";
    private static final List<String> PROFILE_FIELDS = Arrays.asList("name", "email", "uid", "photoUrl");

    private AuthProfileSync() { }

    static Task<Void> ensureProfile(@NonNull FirebaseUser user) {
        String uid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseReference realtimeRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("users").child(uid);

        Task<DocumentSnapshot> firestoreRead = firestore.collection("users").document(uid).get();
        Task<DataSnapshot> realtimeRead = realtimeRef.get();

        return Tasks.whenAllSuccess(firestoreRead, realtimeRead).continueWithTask(readTask -> {
            if (!readTask.isSuccessful()) {
                Exception cause = readTask.getException();
                if (cause != null) throw cause;
                throw new IllegalStateException("Could not load account profile.");
            }

            @SuppressWarnings("unchecked")
            DocumentSnapshot firestoreProfile = (DocumentSnapshot) readTask.getResult().get(0);
            DataSnapshot realtimeProfile = (DataSnapshot) readTask.getResult().get(1);

            Map<String, Object> profile = new HashMap<>();
            for (String field : PROFILE_FIELDS) {
                Object value = realtimeProfile.child(field).getValue();
                if (value == null && firestoreProfile.exists()) {
                    value = firestoreProfile.get(field);
                }
                if (value instanceof String && !((String) value).trim().isEmpty()) {
                    profile.put(field, ((String) value).trim());
                }
            }

            String email = user.getEmail();
            if (email != null && !email.trim().isEmpty()) profile.put("email", email.trim());
            profile.put("uid", uid);

            String name = nonEmptyString(profile.get("name"));
            if (name == null) name = nonEmptyString(user.getDisplayName());
            if (name == null && email != null && email.contains("@")) name = email.substring(0, email.indexOf('@'));
            if (name == null) name = "User";
            profile.put("name", name);

            String photoUrl = user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString();
            if (photoUrl != null && !photoUrl.isEmpty()) profile.put("photoUrl", photoUrl);

            Task<Void> firestoreWrite = firestore.collection("users").document(uid)
                    .set(profile, SetOptions.merge());
            Task<Void> realtimeWrite = realtimeRef.updateChildren(profile);
            return Tasks.whenAll(firestoreWrite, realtimeWrite);
        });
    }

    private static String nonEmptyString(Object value) {
        if (!(value instanceof String)) return null;
        String text = ((String) value).trim();
        return text.isEmpty() ? null : text;
    }
}
