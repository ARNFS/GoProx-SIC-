package com.example.goprox;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class AddPostActivity extends BaseActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_PROFESSION_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_COUNTRY_LENGTH = 60;
    private static final int MAX_CITY_LENGTH = 60;
    private static final double MAX_PRICE = 99999;
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final String[] FORBIDDEN_WORDS = {
            "fuck",
            "shit",
            "porn",
            "sex",
            "nigger",
            "nazi"
    };

    private static final Set<String> STOP_WORDS = new HashSet<>();

    private static final String[] ALLOWED_IMAGE_MIME_TYPES = {
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    };

    /*
     * IMPORTANT:
     * Do NOT use Pattern.UNICODE_CHARACTER_CLASS here.
     * Some Android runtimes do not support that flag and the class
     * initialization crashes before onCreate().
     */
    private static final Pattern SERVICE_TEXT_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{N}\\s.,!?()&+/#'’\\-:]+$"
    );

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{N}\\s.'’\\-]+$"
    );

    private TextInputEditText etName;
    private TextInputEditText etProfession;
    private TextInputEditText etDescription;
    private TextInputEditText etPrice;
    private TextInputEditText etCountry;
    private TextInputEditText etCity;

    private Spinner spinnerPriceType;

    private MaterialButton btnSelectImage;
    private MaterialButton btnSubmit;
    private ImageView ivServiceImage;

    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private String userId;

    private Uri imageUri;

    private boolean isEditMode = false;
    private boolean isSubmitting = false;

    private String editServiceId;
    private String existingImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_post);

        initializeViews();
        initializeFirebase();
        setupToolbar();
        setupPriceSpinner();
        setupInputFilters();
        setupListeners();
        setupBottomNavigation();
        setupBackHandling();
        checkEditMode();
    }

    private void initializeViews() {
        ivServiceImage = findViewById(R.id.ivServiceImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        etName = findViewById(R.id.etName);
        etProfession = findViewById(R.id.etProfession);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etCountry = findViewById(R.id.etCountry);
        etCity = findViewById(R.id.etCity);

        spinnerPriceType = findViewById(R.id.spinnerPriceType);

        btnSubmit = findViewById(R.id.btnSubmit);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        userId = auth.getCurrentUser().getUid();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar =
                findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            toolbar.setNavigationOnClickListener(v ->
                    handleBackNavigation()
            );
        }
    }

    private void setupPriceSpinner() {
        String[] priceTypes = {
                "$ / hour",
                "Fixed",
                "Depends on problem"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priceTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPriceType.setAdapter(adapter);
    }

    private void setupInputFilters() {

        if (etName != null) {
            etName.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(MAX_NAME_LENGTH)
            });
        }

        if (etProfession != null) {
            etProfession.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(MAX_PROFESSION_LENGTH)
            });
        }

        if (etDescription != null) {
            etDescription.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(MAX_DESCRIPTION_LENGTH)
            });
        }

        if (etCountry != null) {
            etCountry.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(MAX_COUNTRY_LENGTH)
            });
        }

        if (etCity != null) {
            etCity.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(MAX_CITY_LENGTH)
            });
        }

        if (etPrice != null) {
            etPrice.setInputType(
                    InputType.TYPE_CLASS_NUMBER |
                            InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
        }
    }

    private void setupListeners() {

        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v ->
                    openImagePicker()
            );
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v ->
                    addService()
            );
        }
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackNavigation();
                    }
                }
        );
    }

    private void handleBackNavigation() {

        if (isSubmitting) {
            Toast.makeText(
                    this,
                    "Please wait until the operation finishes",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        finish();
    }

    private void checkEditMode() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        editServiceId = intent.getStringExtra("serviceId");

        if (editServiceId == null || editServiceId.trim().isEmpty()) {
            return;
        }

        isEditMode = true;

        if (btnSubmit != null) {
            btnSubmit.setText("Update Service");
        }

        androidx.appcompat.widget.Toolbar toolbar =
                findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.setTitle("Edit Service");
        }

        String name = intent.getStringExtra("name");
        String profession = intent.getStringExtra("profession");
        String description = intent.getStringExtra("description");
        String price = intent.getStringExtra("price");
        String priceType = intent.getStringExtra("priceType");
        String country = intent.getStringExtra("country");
        String city = intent.getStringExtra("city");
        existingImageUrl = intent.getStringExtra("imageUrl");

        if (name != null) {
            etName.setText(name);
        }

        if (profession != null) {
            etProfession.setText(profession);
        }

        if (description != null) {
            etDescription.setText(description);
        }

        if (price != null) {
            etPrice.setText(price);
        }

        if (country != null) {
            etCountry.setText(country);
        }

        if (city != null) {
            etCity.setText(city);
        }

        if (priceType != null && spinnerPriceType != null) {

            ArrayAdapter<String> adapter =
                    (ArrayAdapter<String>) spinnerPriceType.getAdapter();

            if (adapter != null) {
                int position = adapter.getPosition(priceType);

                if (position >= 0) {
                    spinnerPriceType.setSelection(position);
                }
            }
        }

        if (existingImageUrl != null &&
                !existingImageUrl.trim().isEmpty() &&
                ivServiceImage != null) {

            Glide.with(this)
                    .load(existingImageUrl)
                    .centerCrop()
                    .into(ivServiceImage);
        }
    }

    private void openImagePicker() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(
                Intent.createChooser(intent, "Select image"),
                PICK_IMAGE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != PICK_IMAGE_REQUEST ||
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {
            return;
        }

        Uri selectedUri = data.getData();

        if (!isValidImage(selectedUri)) {
            return;
        }

        imageUri = selectedUri;

        if (ivServiceImage != null) {
            Glide.with(this)
                    .load(imageUri)
                    .centerCrop()
                    .into(ivServiceImage);
        }
    }

    private boolean isValidImage(Uri uri) {

        if (uri == null) {
            showError("Invalid image");
            return false;
        }

        String mimeType = getContentResolver()
                .getType(uri);

        if (mimeType == null) {
            showError("Could not determine image type");
            return false;
        }

        boolean allowedType = false;

        for (String allowed : ALLOWED_IMAGE_MIME_TYPES) {
            if (allowed.equalsIgnoreCase(mimeType)) {
                allowedType = true;
                break;
            }
        }

        if (!allowedType) {
            showError(
                    "Only JPG, PNG and WebP images are allowed"
            );
            return false;
        }

        long fileSize = getFileSize(uri);

        if (fileSize > MAX_IMAGE_SIZE_BYTES) {
            showError("Image must be smaller than 5 MB");
            return false;
        }

        return true;
    }

    private long getFileSize(Uri uri) {

        try {
            android.database.Cursor cursor =
                    getContentResolver().query(
                            uri,
                            null,
                            null,
                            null,
                            null
                    );

            if (cursor != null) {

                int sizeIndex =
                        cursor.getColumnIndex(
                                android.provider.OpenableColumns.SIZE
                        );

                if (cursor.moveToFirst() &&
                        sizeIndex >= 0 &&
                        !cursor.isNull(sizeIndex)) {

                    long size = cursor.getLong(sizeIndex);

                    cursor.close();

                    return size;
                }

                cursor.close();
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private void addService() {

        if (isSubmitting) {
            return;
        }

        if (auth == null ||
                auth.getCurrentUser() == null) {

            showError("Please log in again");
            return;
        }

        String name = getText(etName);
        String profession = getText(etProfession);
        String description = getText(etDescription);
        String priceText = getText(etPrice);
        String country = getText(etCountry);
        String city = getText(etCity);

        String priceType = "";

        if (spinnerPriceType != null &&
                spinnerPriceType.getSelectedItem() != null) {

            priceType =
                    spinnerPriceType
                            .getSelectedItem()
                            .toString()
                            .trim();
        }

        if (!validateService(
                name,
                profession,
                description,
                priceText,
                country,
                city
        )) {
            return;
        }

        double price;

        try {
            price = Double.parseDouble(
                    priceText.replace(",", ".")
            );
        } catch (Exception e) {
            showError("Invalid price");
            return;
        }

        if (price < 0 || price > MAX_PRICE) {
            showError("Price must be between 0 and 99999");
            return;
        }

        setSubmittingState(true);

        if (imageUri != null) {
            uploadImageAndSave(
                    name,
                    profession,
                    description,
                    price,
                    priceType,
                    country,
                    city
            );
        } else {
            saveServiceToFirestore(
                    name,
                    profession,
                    description,
                    price,
                    priceType,
                    country,
                    city,
                    existingImageUrl
            );
        }
    }

    private boolean validateService(
            String name,
            String profession,
            String description,
            String priceText,
            String country,
            String city
    ) {

        if (name.isEmpty()) {
            showError("Enter service name");
            return false;
        }

        if (profession.isEmpty()) {
            showError("Enter profession");
            return false;
        }

        if (description.isEmpty()) {
            showError("Enter service description");
            return false;
        }

        if (priceText.isEmpty()) {
            showError("Enter price");
            return false;
        }

        if (country.isEmpty()) {
            showError("Enter country");
            return false;
        }

        if (city.isEmpty()) {
            showError("Enter city");
            return false;
        }

        if (!SERVICE_TEXT_PATTERN.matcher(name).matches()) {
            showError("Service name contains invalid characters");
            return false;
        }

        if (!SERVICE_TEXT_PATTERN.matcher(profession).matches()) {
            showError("Profession contains invalid characters");
            return false;
        }

        if (!SERVICE_TEXT_PATTERN.matcher(description).matches()) {
            showError("Description contains invalid characters");
            return false;
        }

        if (!LOCATION_PATTERN.matcher(country).matches()) {
            showError("Country contains invalid characters");
            return false;
        }

        if (!LOCATION_PATTERN.matcher(city).matches()) {
            showError("City contains invalid characters");
            return false;
        }

        if (containsForbiddenWord(name) ||
                containsForbiddenWord(profession) ||
                containsForbiddenWord(description)) {

            showError("Please use appropriate language");
            return false;
        }

        return true;
    }

    private boolean containsForbiddenWord(String text) {

        if (text == null) {
            return false;
        }

        String normalized =
                text.toLowerCase(Locale.ROOT);

        for (String word : FORBIDDEN_WORDS) {

            if (normalized.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private void uploadImageAndSave(
            String name,
            String profession,
            String description,
            double price,
            String priceType,
            String country,
            String city
    ) {

        if (imageUri == null) {
            saveServiceToFirestore(
                    name,
                    profession,
                    description,
                    price,
                    priceType,
                    country,
                    city,
                    existingImageUrl
            );

            return;
        }

        String fileName =
                userId + "_" +
                        System.currentTimeMillis() +
                        ".jpg";

        StorageReference imageRef =
                storage.getReference()
                        .child("service_images")
                        .child(userId)
                        .child(fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri ->
                                        saveServiceToFirestore(
                                                name,
                                                profession,
                                                description,
                                                price,
                                                priceType,
                                                country,
                                                city,
                                                downloadUri.toString()
                                        )
                                )
                                .addOnFailureListener(e -> {
                                    setSubmittingState(false);
                                    showError(
                                            "Failed to get image URL"
                                    );
                                })
                )
                .addOnFailureListener(e -> {
                    setSubmittingState(false);
                    showError(
                            "Failed to upload image"
                    );
                });
    }

    private void saveServiceToFirestore(
            String name,
            String profession,
            String description,
            double price,
            String priceType,
            String country,
            String city,
            String imageUrl
    ) {

        if (firestore == null ||
                userId == null ||
                userId.trim().isEmpty()) {

            setSubmittingState(false);
            showError("Database is not available");
            return;
        }

        Map<String, Object> service = new HashMap<>();

        service.put("userId", userId);
        service.put("name", name);
        service.put("profession", profession);
        service.put("description", description);
        service.put("price", price);
        service.put("priceType", priceType);
        service.put("country", country);
        service.put("city", city);

        if (imageUrl != null &&
                !imageUrl.trim().isEmpty()) {

            service.put("imageUrl", imageUrl);
        }

        service.put(
                "tags",
                generateTags(
                        name,
                        profession,
                        description,
                        country,
                        city
                )
        );

        if (isEditMode &&
                editServiceId != null &&
                !editServiceId.trim().isEmpty()) {

            DocumentReference serviceRef =
                    firestore.collection("services")
                            .document(editServiceId);

            service.put(
                    "updatedAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp()
            );

            serviceRef.set(
                            service,
                            SetOptions.merge()
                    )
                    .addOnSuccessListener(unused -> {
                        setSubmittingState(false);

                        Toast.makeText(
                                this,
                                "Service updated successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        navigateHome();
                    })
                    .addOnFailureListener(e -> {
                        setSubmittingState(false);

                        showError(
                                "Failed to update service: " +
                                        e.getMessage()
                        );
                    });

        } else {

            service.put("rating", 0.0);
            service.put("ratingCount", 0L);

            service.put(
                    "createdAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp()
            );

            service.put(
                    "updatedAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp()
            );

            firestore.collection("services")
                    .add(service)
                    .addOnSuccessListener(documentReference -> {

                        setSubmittingState(false);

                        Toast.makeText(
                                this,
                                "Service added successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        navigateHome();
                    })
                    .addOnFailureListener(e -> {

                        setSubmittingState(false);

                        showError(
                                "Failed to save service: " +
                                        e.getMessage()
                        );
                    });
        }
    }

    private List<String> generateTags(
            String name,
            String profession,
            String description,
            String country,
            String city
    ) {

        Set<String> tags = new HashSet<>();

        addWordsToTags(tags, name);
        addWordsToTags(tags, profession);
        addWordsToTags(tags, description);
        addWordsToTags(tags, country);
        addWordsToTags(tags, city);

        return new ArrayList<>(tags);
    }

    private void addWordsToTags(
            Set<String> tags,
            String text
    ) {

        if (text == null) {
            return;
        }

        String normalized =
                text.toLowerCase(Locale.ROOT);

        String[] words =
                normalized.split("\\s+");

        for (String word : words) {

            word = word
                    .replaceAll(
                            "[^\\p{L}\\p{N}]",
                            ""
                    )
                    .trim();

            if (word.isEmpty()) {
                continue;
            }

            if (STOP_WORDS.contains(word)) {
                continue;
            }

            if (word.length() >= 2) {
                tags.add(word);
            }
        }
    }

    private String getText(EditText editText) {

        if (editText == null ||
                editText.getText() == null) {

            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private void setSubmittingState(boolean submitting) {

        isSubmitting = submitting;

        if (btnSubmit != null) {
            btnSubmit.setEnabled(!submitting);

            if (submitting) {
                btnSubmit.setText(
                        isEditMode
                                ? "Updating..."
                                : "Adding..."
                );
            } else {
                btnSubmit.setText(
                        isEditMode
                                ? "Update Service"
                                : "Add Service"
                );
            }
        }

        if (btnSelectImage != null) {
            btnSelectImage.setEnabled(!submitting);
        }
    }

    private void showError(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void navigateHome() {

        Intent intent =
                new Intent(
                        this,
                        HomeActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {

        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(
                item -> {

                    int id = item.getItemId();

                    if (id == R.id.nav_home) {

                        startActivity(
                                new Intent(
                                        this,
                                        HomeActivity.class
                                )
                        );

                        finish();

                        return true;

                    } else if (id == R.id.nav_chats) {

                        startActivity(
                                new Intent(
                                        this,
                                        ChatListActivity.class
                                )
                        );

                        finish();

                        return true;

                    } else if (id == R.id.nav_add) {

                        return true;

                    } else if (id == R.id.nav_profile) {

                        startActivity(
                                new Intent(
                                        this,
                                        ProfileActivity.class
                                )
                        );

                        finish();

                        return true;
                    }

                    return false;
                }
        );

        bottomNavigationView.setSelectedItemId(
                R.id.nav_add
        );
    }
}