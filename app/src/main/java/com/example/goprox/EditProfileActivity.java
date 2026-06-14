package com.example.goprox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePhoto;
    private EditText etName;
    private TextView tvEmail;
    private Button btnChangePhoto, btnSave;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final String FIREBASE_DB_URL = "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (currentUser == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadCurrentData();
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.ivEditProfilePhoto);
        etName = findViewById(R.id.etEditName);
        tvEmail = findViewById(R.id.tvEditEmail);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSave = findViewById(R.id.btnSaveProfile);

        if (btnChangePhoto == null || btnSave == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnChangePhoto.setOnClickListener(v -> chooseImage());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadCurrentData() {
        if (currentUser == null) return;

        etName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
        tvEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email");

        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivProfilePhoto);
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfilePhoto.setImageURI(imageUri);
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        db.collection("users").document(currentUser.getUid()).update(userData);

                        // 🔥 Update Realtime Database — name
                        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                                .getReference("users").child(currentUser.getUid()).child("name")
                                .setValue(name);

                        if (imageUri != null) {
                            uploadProfilePhoto(name);
                        } else {
                            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfilePhoto(String name) {
        String fileName = "profile_photos/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = storage.getReference().child(fileName);

        fileRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return fileRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String photoUrl = task.getResult().toString();

                        UserProfileChangeRequest photoUpdate = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(Uri.parse(photoUrl))
                                .build();

                        currentUser.updateProfile(photoUpdate)
                                .addOnCompleteListener(photoTask -> {
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("photoUrl", photoUrl);
                                    userData.put("name", name);
                                    db.collection("users").document(currentUser.getUid()).update(userData);

                                    // 🔥 Update Realtime Database — name + photoUrl
                                    Map<String, Object> rtData = new HashMap<>();
                                    rtData.put("name", name);
                                    rtData.put("photoUrl", photoUrl);
                                    FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                                            .getReference("users").child(currentUser.getUid())
                                            .updateChildren(rtData);

                                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    } else {
                        Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                    }
                });
    }
}