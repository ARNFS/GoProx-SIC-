package com.example.goprox;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class AddPostActivity extends BaseActivity {

    private EditText etName, etProfession, etDescription, etPrice;
    private EditText etCountry, etCity;
    private Spinner spinnerPriceType;
    private Button btnSubmit, btnSelectImage;
    private ImageView ivServiceImage;
    private BottomNavigationView bottomNavigationView;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String userId;

    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    // 🔥 EDIT MODE
    private boolean isEditMode = false;
    private String editServiceId;
    private String existingImageUrl;

    private static final List<String> FORBIDDEN_WORDS = Arrays.asList(
            "sex", "porn", "fuck", "shit", "damn", "cock", "dick", "pussy",
            "asshole", "bitch", "whore", "slut", "cunt", "motherfucker"
    );

    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "am", "be",
            "been", "being", "have", "has", "had", "do", "does", "did", "will",
            "would", "shall", "should", "can", "could", "may", "might", "must"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Service");
        }

        etName = findViewById(R.id.etName);
        etProfession = findViewById(R.id.etProfession);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        spinnerPriceType = findViewById(R.id.spinnerPriceType);
        etCountry = findViewById(R.id.etCountry);
        etCity = findViewById(R.id.etCity);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivServiceImage = findViewById(R.id.ivServiceImage);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String[] priceTypes = {"$/hour", "Fixed", "Depends on problem"};
        ArrayAdapter<String> priceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priceTypes);
        priceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriceType.setAdapter(priceAdapter);

        spinnerPriceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("Depends on problem")) {
                    etPrice.setEnabled(false);
                    etPrice.setText("");
                    etPrice.setHint("Not required");
                } else {
                    etPrice.setEnabled(true);
                    etPrice.setHint("Price");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPrice.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});

        InputFilter letterFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetter(c) && !Character.isSpaceChar(c)) return "";
            }
            return null;
        };
        etName.setFilters(new InputFilter[]{letterFilter, new InputFilter.LengthFilter(50)});
        etProfession.setFilters(new InputFilter[]{letterFilter, new InputFilter.LengthFilter(50)});
        etDescription.setFilters(new InputFilter[]{letterFilter, new InputFilter.LengthFilter(200)});

        btnSelectImage.setOnClickListener(v -> openFileChooser());
        btnSubmit.setOnClickListener(v -> addService());
        setupBottomNavigation();

        // 🔥 CHECK EDIT MODE
        checkEditMode();
    }

    // 🔥 CHECK IF EDIT MODE
    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent.hasExtra("serviceId")) {
            isEditMode = true;
            editServiceId = intent.getStringExtra("serviceId");
            getSupportActionBar().setTitle("Edit Service");
            btnSubmit.setText("Update Service");

            // Load existing data
            etName.setText(intent.getStringExtra("name"));
            etProfession.setText(intent.getStringExtra("profession"));
            etDescription.setText(intent.getStringExtra("description"));

            String price = intent.getStringExtra("price");
            if (price != null) {
                String priceNum = price.replaceAll("[^0-9]", "");
                etPrice.setText(priceNum);
                if (price.contains("$")) {
                    spinnerPriceType.setSelection(price.contains("/hour") ? 0 : 1);
                }
            }

            etCountry.setText(intent.getStringExtra("country"));
            etCity.setText(intent.getStringExtra("city"));
            existingImageUrl = intent.getStringExtra("imageUrl");

            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                Glide.with(this).load(existingImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivServiceImage);
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivServiceImage.setImageURI(imageUri);
        }
    }

    private void addService() {
        btnSubmit.setEnabled(false);
        btnSubmit.setText(isEditMode ? "Updating..." : "Adding...");

        String name = etName.getText().toString().trim();
        String profession = etProfession.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceNumber = etPrice.getText().toString().trim();
        String priceType = spinnerPriceType.getSelectedItem().toString();
        String country = etCountry.getText().toString().trim();
        String city = etCity.getText().toString().trim();

        if (name.isEmpty() || profession.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true); btnSubmit.setText(isEditMode ? "Update Service" : "Add Service"); return;
        }

        if (!priceType.equals("Depends on problem") && priceNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true); btnSubmit.setText(isEditMode ? "Update Service" : "Add Service"); return;
        }

        if (containsForbiddenWord(name) || containsForbiddenWord(profession) || containsForbiddenWord(description)) {
            Toast.makeText(this, "Please avoid inappropriate words", Toast.LENGTH_LONG).show();
            btnSubmit.setEnabled(true); btnSubmit.setText(isEditMode ? "Update Service" : "Add Service"); return;
        }

        if (!isValidText(name) || !isValidText(profession) || !isValidText(description)) {
            Toast.makeText(this, "Use only letters and spaces (no numbers/symbols)", Toast.LENGTH_LONG).show();
            btnSubmit.setEnabled(true); btnSubmit.setText(isEditMode ? "Update Service" : "Add Service"); return;
        }

        String priceFormatted;
        if (priceType.equals("Depends on problem")) {
            priceFormatted = "Depends on problem";
        } else if (priceNumber.isEmpty()) {
            priceFormatted = "0";
        } else {
            try {
                int priceInt = Integer.parseInt(priceNumber);
                if (priceInt <= 0) {
                    Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true); btnSubmit.setText(isEditMode ? "Update Service" : "Add Service"); return;
                }
                priceFormatted = priceNumber + (priceType.equals("Fixed") ? "$" : "$/hour");
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true); btnSubmit.setText(isEditMode ? "Update Service" : "Add Service"); return;
            }
        }

        if (imageUri != null) {
            uploadImageAndSave(name, profession, description, priceFormatted, priceType, country, city);
        } else {
            saveServiceToFirestore(name, profession, description, priceFormatted, priceType, existingImageUrl, country, city);
        }
    }

    private void uploadImageAndSave(String name, String profession, String description,
                                    String priceFormatted, String priceType, String country, String city) {
        String fileExtension = getFileExtension(imageUri);
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        StorageReference fileRef = storageRef.child("service_images/" + fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveServiceToFirestore(name, profession, description, priceFormatted, priceType, uri.toString(), country, city);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveServiceToFirestore(name, profession, description, priceFormatted, priceType, existingImageUrl, country, city);
                });
    }

    private void saveServiceToFirestore(String name, String profession, String description,
                                        String priceFormatted, String priceType, String imageUrl,
                                        String country, String city) {
        List<String> tags = generateTags(profession, description);

        Map<String, Object> service = new HashMap<>();
        service.put("name", name); service.put("profession", profession); service.put("description", description);
        service.put("price", priceFormatted); service.put("priceType", priceType);
        service.put("rating", 0.0); service.put("ratingCount", 0); service.put("userId", userId);
        service.put("tags", tags); service.put("createdAt", System.currentTimeMillis()); service.put("imageUrl", imageUrl);
        if (country != null && !country.isEmpty()) service.put("country", country);
        if (city != null && !city.isEmpty()) service.put("city", city);

        if (isEditMode && editServiceId != null) {
            // 🔥 UPDATE
            service.put("updatedAt", System.currentTimeMillis());
            db.collection("services").document(editServiceId).update(service)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Service updated!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true); btnSubmit.setText("Update Service");
                    });
        } else {
            // 🔥 ADD NEW
            db.collection("services").add(service)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "✅ Service added!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true); btnSubmit.setText("Add Service");
                    });
        }
    }

    private boolean containsForbiddenWord(String text) {
        String lower = text.toLowerCase();
        for (String word : FORBIDDEN_WORDS) if (lower.contains(word)) return true;
        return false;
    }

    private boolean isValidText(String text) {
        return Pattern.compile("^[a-zA-Z\\s]+$").matcher(text).matches();
    }

    private List<String> generateTags(String profession, String description) {
        List<String> tags = new ArrayList<>(); tags.add(profession.toLowerCase());
        for (String word : description.toLowerCase().split("[ ,.?!:;()\\[\\]{}]+")) if (word.length() > 3 && !STOP_WORDS.contains(word)) tags.add(word);
        String prof = profession.toLowerCase();
        if (prof.contains("php")) tags.addAll(Arrays.asList("backend", "server", "web", "database"));
        else if (prof.contains("ios")) tags.addAll(Arrays.asList("mobile", "apple", "swift", "iphone"));
        else if (prof.contains("android")) tags.addAll(Arrays.asList("mobile", "java", "kotlin", "google"));
        else if (prof.contains("electrician")) tags.addAll(Arrays.asList("electrical", "wiring", "repair", "maintenance"));
        else if (prof.contains("plumber")) tags.addAll(Arrays.asList("pipe", "leak", "water", "repair"));
        else if (prof.contains("developer") || prof.contains("programmer")) tags.addAll(Arrays.asList("coding", "software", "development"));
        List<String> unique = new ArrayList<>(); for (String t : tags) if (!unique.contains(t)) unique.add(t);
        return unique;
    }

    private String getFileExtension(Uri uri) {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, HomeActivity.class)); finish(); return true; }
            else if (id == R.id.nav_chats) { startActivity(new Intent(this, ChatListActivity.class)); finish(); return true; }
            else if (id == R.id.nav_add) { return true; }
            else if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); finish(); return true; }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}