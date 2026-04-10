package com.example.goprox;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private Button btnSubmit, btnSelectImage;
    private ImageView ivServiceImage;
    private BottomNavigationView bottomNavigationView;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String userId;

    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

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
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivServiceImage = findViewById(R.id.ivServiceImage);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPrice.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        InputFilter letterFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (!Character.isLetter(c) && !Character.isSpaceChar(c)) {
                        return "";
                    }
                }
                return null;
            }
        };
        etName.setFilters(new InputFilter[]{letterFilter, new InputFilter.LengthFilter(50)});
        etProfession.setFilters(new InputFilter[]{letterFilter, new InputFilter.LengthFilter(50)});
        etDescription.setFilters(new InputFilter[]{letterFilter, new InputFilter.LengthFilter(200)});

        btnSelectImage.setOnClickListener(v -> openFileChooser());
        btnSubmit.setOnClickListener(v -> addService());
        setupBottomNavigation();
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivServiceImage.setImageURI(imageUri);
        }
    }

    private void addService() {
        String name = etName.getText().toString().trim();
        String profession = etProfession.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceNumber = etPrice.getText().toString().trim();

        if (name.isEmpty() || profession.isEmpty() || description.isEmpty() || priceNumber.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (containsForbiddenWord(name) || containsForbiddenWord(profession) || containsForbiddenWord(description)) {
            Toast.makeText(this, "Please avoid inappropriate words", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isValidText(name) || !isValidText(profession) || !isValidText(description)) {
            Toast.makeText(this, "Use only letters and spaces (no numbers/symbols)", Toast.LENGTH_LONG).show();
            return;
        }

        int priceInt;
        try {
            priceInt = Integer.parseInt(priceNumber);
            if (priceInt <= 0) {
                Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }
        String priceFormatted = priceInt + "$/hour";

        if (imageUri == null) {
            saveServiceToFirestore(name, profession, description, priceFormatted, null);
        } else {
            uploadImageAndSave(name, profession, description, priceFormatted);
        }
    }

    private void uploadImageAndSave(String name, String profession, String description, String priceFormatted) {
        String fileExtension = getFileExtension(imageUri);
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        StorageReference fileRef = storageRef.child("service_images/" + fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveServiceToFirestore(name, profession, description, priceFormatted, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveServiceToFirestore(name, profession, description, priceFormatted, null);
                });
    }

    private void saveServiceToFirestore(String name, String profession, String description,
                                        String priceFormatted, String imageUrl) {
        List<String> tags = generateTags(profession, description);

        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("profession", profession);
        service.put("description", description);
        service.put("price", priceFormatted);
        service.put("rating", 0.0);
        service.put("ratingCount", 0);
        service.put("userId", userId);
        service.put("tags", tags);
        service.put("createdAt", System.currentTimeMillis());
        service.put("imageUrl", imageUrl);

        db.collection("services")
                .add(service)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "✅ Service added!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean containsForbiddenWord(String text) {
        String lower = text.toLowerCase();
        for (String word : FORBIDDEN_WORDS) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidText(String text) {
        Pattern pattern = Pattern.compile("^[a-zA-Z\\s]+$");
        return pattern.matcher(text).matches();
    }

    private List<String> generateTags(String profession, String description) {
        List<String> tags = new ArrayList<>();
        tags.add(profession.toLowerCase());

        String[] words = description.toLowerCase().split("[ ,.?!:;()\\[\\]{}]+");
        for (String word : words) {
            if (word.length() > 3 && !STOP_WORDS.contains(word)) {
                tags.add(word);
            }
        }

        String profLower = profession.toLowerCase();
        if (profLower.contains("php")) {
            tags.addAll(Arrays.asList("backend", "server", "web", "database"));
        } else if (profLower.contains("ios")) {
            tags.addAll(Arrays.asList("mobile", "apple", "swift", "iphone"));
        } else if (profLower.contains("android")) {
            tags.addAll(Arrays.asList("mobile", "java", "kotlin", "google"));
        } else if (profLower.contains("electrician")) {
            tags.addAll(Arrays.asList("electrical", "wiring", "repair", "maintenance"));
        } else if (profLower.contains("plumber")) {
            tags.addAll(Arrays.asList("pipe", "leak", "water", "repair"));
        } else if (profLower.contains("developer") || profLower.contains("programmer")) {
            tags.addAll(Arrays.asList("coding", "software", "development"));
        }

        List<String> unique = new ArrayList<>();
        for (String t : tags) {
            if (!unique.contains(t)) unique.add(t);
        }
        return unique;
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_chats) {
                startActivity(new Intent(this, ChatListActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}