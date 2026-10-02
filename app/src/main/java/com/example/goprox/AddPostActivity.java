package com.example.goprox;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.InputType;
import android.webkit.MimeTypeMap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class AddPostActivity extends BaseActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_PROFESSION_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_COUNTRY_LENGTH = 60;
    private static final int MAX_CITY_LENGTH = 60;

    private static final int MAX_PRICE = 99999;

    private static final long MAX_IMAGE_SIZE_BYTES =
            5L * 1024L * 1024L;

    private static final List<String> FORBIDDEN_WORDS = Arrays.asList(
            "sex",
            "porn",
            "fuck",
            "shit",
            "damn",
            "cock",
            "dick",
            "pussy",
            "asshole",
            "bitch",
            "whore",
            "slut",
            "cunt",
            "motherfucker"
    );

    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "as", "is", "was", "are",
            "am", "be", "been", "being", "have", "has", "had", "do", "does",
            "did", "will", "would", "shall", "should", "can", "could",
            "may", "might", "must"
    );

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES =
            new HashSet<>(Arrays.asList(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/webp"
            ));

    private static final Pattern SERVICE_TEXT_PATTERN =
            Pattern.compile(
                    "^[\\p{L}\\p{N}\\s.,!?()&+/#'’\\-:]+$",
                    Pattern.UNICODE_CHARACTER_CLASS
            );

    private static final Pattern LOCATION_PATTERN =
            Pattern.compile(
                    "^[\\p{L}\\p{N}\\s.'’\\-]+$",
                    Pattern.UNICODE_CHARACTER_CLASS
            );

    private EditText etName;
    private EditText etProfession;
    private EditText etDescription;
    private EditText etPrice;
    private EditText etCountry;
    private EditText etCity;

    private Spinner spinnerPriceType;

    private Button btnSubmit;
    private Button btnSelectImage;

    private ImageView ivServiceImage;

    private BottomNavigationView bottomNavigationView;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

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

        etName = findViewById(R.id.etName);
        etProfession = findViewById(R.id.etProfession);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);

        etCountry = findViewById(R.id.etCountry);
        etCity = findViewById(R.id.etCity);

        spinnerPriceType = findViewById(R.id.spinnerPriceType);

        btnSubmit = findViewById(R.id.btnSubmit);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        ivServiceImage = findViewById(R.id.ivServiceImage);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void initializeFirebase() {

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please sign in first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        userId = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();
    }

    private void setupToolbar() {

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);

            getSupportActionBar()
                    .setTitle("Add Service");
        }
    }

    private void setupPriceSpinner() {

        String[] priceTypes = {
                "$/hour",
                "Fixed",
                "Depends on problem"
        };

        ArrayAdapter<String> priceAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        priceTypes
                );

        priceAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPriceType.setAdapter(priceAdapter);

        spinnerPriceType.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        String selected =
                                String.valueOf(
                                        parent.getItemAtPosition(position)
                                );

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
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    private void setupInputFilters() {

        etName.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        etProfession.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        etDescription.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        etCountry.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        etCity.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        etPrice.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        etName.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(
                                MAX_NAME_LENGTH
                        )
                }
        );

        etProfession.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(
                                MAX_PROFESSION_LENGTH
                        )
                }
        );

        etDescription.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(
                                MAX_DESCRIPTION_LENGTH
                        )
                }
        );

        etCountry.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(
                                MAX_COUNTRY_LENGTH
                        )
                }
        );

        etCity.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(
                                MAX_CITY_LENGTH
                        )
                }
        );

        etPrice.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(5)
                }
        );
    }

    private void setupListeners() {

        btnSelectImage.setOnClickListener(
                v -> openFileChooser()
        );

        btnSubmit.setOnClickListener(
                v -> addService()
        );
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

            new AlertDialog.Builder(this)
                    .setTitle("Upload in progress")
                    .setMessage(
                            "The service is being saved. Are you sure you want to leave?"
                    )
                    .setNegativeButton(
                            "Stay",
                            null
                    )
                    .setPositiveButton(
                            "Leave",
                            (dialog, which) -> finish()
                    )
                    .show();

            return;
        }

        finish();
    }

    private void checkEditMode() {

        Intent intent = getIntent();

        if (intent == null ||
                !intent.hasExtra("serviceId")) {

            return;
        }

        editServiceId =
                intent.getStringExtra("serviceId");

        if (editServiceId == null ||
                editServiceId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid service",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        isEditMode = true;

        if (getSupportActionBar() != null) {

            getSupportActionBar()
                    .setTitle("Edit Service");
        }

        btnSubmit.setText("Update Service");

        String name =
                intent.getStringExtra("name");

        String profession =
                intent.getStringExtra("profession");

        String description =
                intent.getStringExtra("description");

        String country =
                intent.getStringExtra("country");

        String city =
                intent.getStringExtra("city");

        String price =
                intent.getStringExtra("price");

        existingImageUrl =
                intent.getStringExtra("imageUrl");

        if (name != null) {
            etName.setText(name);
        }

        if (profession != null) {
            etProfession.setText(profession);
        }

        if (description != null) {
            etDescription.setText(description);
        }

        if (country != null) {
            etCountry.setText(country);
        }

        if (city != null) {
            etCity.setText(city);
        }

        applyExistingPrice(price);

        if (existingImageUrl != null &&
                !existingImageUrl.trim().isEmpty()) {

            Glide.with(this)
                    .load(existingImageUrl)
                    .placeholder(
                            R.drawable.ic_profile_placeholder
                    )
                    .error(
                            R.drawable.ic_profile_placeholder
                    )
                    .into(ivServiceImage);
        }
    }

    private void applyExistingPrice(String price) {

        if (price == null ||
                price.trim().isEmpty()) {

            return;
        }

        if (price.equalsIgnoreCase(
                "Depends on problem"
        )) {

            spinnerPriceType.setSelection(2);
            etPrice.setText("");

            return;
        }

        String normalizedPrice =
                price.replaceAll(
                        "[^0-9]",
                        ""
                );

        etPrice.setText(normalizedPrice);

        if (price.contains("/hour")) {

            spinnerPriceType.setSelection(0);

        } else if (price.contains("$")) {

            spinnerPriceType.setSelection(1);
        }
    }

    private void openFileChooser() {

        Intent intent =
                new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                intent,
                PICK_IMAGE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
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

        Uri selectedUri =
                data.getData();

        if (!isValidImage(selectedUri)) {
            return;
        }

        imageUri = selectedUri;

        ivServiceImage.setImageURI(
                imageUri
        );
    }

    private boolean isValidImage(Uri uri) {

        if (uri == null) {
            return false;
        }

        ContentResolver resolver =
                getContentResolver();

        String mimeType =
                resolver.getType(uri);

        if (mimeType == null ||
                !ALLOWED_IMAGE_MIME_TYPES.contains(
                        mimeType.toLowerCase(Locale.ROOT)
                )) {

            Toast.makeText(
                    this,
                    "Please select a JPG, PNG or WebP image",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        long fileSize =
                getFileSize(uri);

        if (fileSize <= 0) {

            Toast.makeText(
                    this,
                    "Could not read image",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (fileSize > MAX_IMAGE_SIZE_BYTES) {

            Toast.makeText(
                    this,
                    "Image must be 5 MB or smaller",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        return true;
    }

    private long getFileSize(Uri uri) {

        Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{
                                    OpenableColumns.SIZE
                            },
                            null,
                            null,
                            null
                    );

            if (cursor != null &&
                    cursor.moveToFirst()) {

                int sizeIndex =
                        cursor.getColumnIndex(
                                OpenableColumns.SIZE
                        );

                if (sizeIndex >= 0 &&
                        !cursor.isNull(sizeIndex)) {

                    return cursor.getLong(sizeIndex);
                }
            }

        } catch (Exception ignored) {

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    private void addService() {

        if (isSubmitting) {
            return;
        }

        if (userId == null ||
                userId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Authentication required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String name =
                normalizeText(
                        etName.getText().toString()
                );

        String profession =
                normalizeText(
                        etProfession.getText().toString()
                );

        String description =
                normalizeText(
                        etDescription.getText().toString()
                );

        String priceNumber =
                etPrice.getText().toString().trim();

        String priceType =
                spinnerPriceType
                        .getSelectedItem()
                        .toString();

        String country =
                normalizeLocation(
                        etCountry.getText().toString()
                );

        String city =
                normalizeLocation(
                        etCity.getText().toString()
                );

        String validationError =
                validateInput(
                        name,
                        profession,
                        description,
                        priceNumber,
                        priceType,
                        country,
                        city
                );

        if (validationError != null) {

            Toast.makeText(
                    this,
                    validationError,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String priceFormatted =
                buildFormattedPrice(
                        priceNumber,
                        priceType
                );

        if (priceFormatted == null) {
            return;
        }

        isSubmitting = true;

        setSubmittingState(true);

        if (isEditMode) {

            verifyEditOwnershipAndSave(
                    name,
                    profession,
                    description,
                    priceFormatted,
                    priceType,
                    country,
                    city
            );

        } else {

            if (imageUri != null) {

                uploadImageAndSave(
                        name,
                        profession,
                        description,
                        priceFormatted,
                        priceType,
                        country,
                        city
                );

            } else {

                saveServiceToFirestore(
                        name,
                        profession,
                        description,
                        priceFormatted,
                        priceType,
                        null,
                        country,
                        city
                );
            }
        }
    }

    private String validateInput(
            String name,
            String profession,
            String description,
            String priceNumber,
            String priceType,
            String country,
            String city
    ) {

        if (name.isEmpty()) {
            return "Name is required";
        }

        if (profession.isEmpty()) {
            return "Profession is required";
        }

        if (description.isEmpty()) {
            return "Description is required";
        }

        if (name.length() > MAX_NAME_LENGTH) {
            return "Name is too long";
        }

        if (profession.length() > MAX_PROFESSION_LENGTH) {
            return "Profession is too long";
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            return "Description is too long";
        }

        if (country.length() > MAX_COUNTRY_LENGTH) {
            return "Country is too long";
        }

        if (city.length() > MAX_CITY_LENGTH) {
            return "City is too long";
        }

        if (!SERVICE_TEXT_PATTERN.matcher(name).matches()) {
            return "Name contains unsupported characters";
        }

        if (!SERVICE_TEXT_PATTERN.matcher(profession).matches()) {
            return "Profession contains unsupported characters";
        }

        if (!SERVICE_TEXT_PATTERN.matcher(description).matches()) {
            return "Description contains unsupported characters";
        }

        if (!country.isEmpty() &&
                !LOCATION_PATTERN.matcher(country).matches()) {

            return "Country contains unsupported characters";
        }

        if (!city.isEmpty() &&
                !LOCATION_PATTERN.matcher(city).matches()) {

            return "City contains unsupported characters";
        }

        if (containsForbiddenWord(name) ||
                containsForbiddenWord(profession) ||
                containsForbiddenWord(description)) {

            return "Please remove inappropriate words";
        }

        if (!priceType.equals("Depends on problem")) {

            if (priceNumber.isEmpty()) {
                return "Price is required";
            }

            try {

                int price =
                        Integer.parseInt(priceNumber);

                if (price < 0 ||
                        price > MAX_PRICE) {

                    return "Price must be between 0 and 99999";
                }

            } catch (NumberFormatException e) {

                return "Invalid price";
            }
        }

        return null;
    }

    private String buildFormattedPrice(
            String priceNumber,
            String priceType
    ) {

        if (priceType.equals("Depends on problem")) {
            return "Depends on problem";
        }

        if (priceNumber == null ||
                priceNumber.trim().isEmpty()) {

            return null;
        }

        try {

            int price =
                    Integer.parseInt(priceNumber);

            if (price < 0 ||
                    price > MAX_PRICE) {

                return null;
            }

            if (priceType.equals("$/hour")) {

                return "$" + price + "/hour";

            } else if (priceType.equals("Fixed")) {

                return "$" + price;

            } else {

                return "$" + price;
            }

        } catch (NumberFormatException e) {

            return null;
        }
    }

    private void verifyEditOwnershipAndSave(
            String name,
            String profession,
            String description,
            String priceFormatted,
            String priceType,
            String country,
            String city
    ) {

        db.collection("services")
                .document(editServiceId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        showError(
                                "Service no longer exists"
                        );

                        return;
                    }

                    String ownerId =
                            document.getString("userId");

                    if (ownerId == null ||
                            !ownerId.equals(userId)) {

                        showError(
                                "You cannot edit this service"
                        );

                        return;
                    }

                    if (imageUri != null) {

                        uploadImageAndSave(
                                name,
                                profession,
                                description,
                                priceFormatted,
                                priceType,
                                country,
                                city
                        );

                    } else {

                        saveServiceToFirestore(
                                name,
                                profession,
                                description,
                                priceFormatted,
                                priceType,
                                existingImageUrl,
                                country,
                                city
                        );
                    }
                })
                .addOnFailureListener(e ->
                        showError(
                                "Could not verify service ownership"
                        )
                );
    }

    private void uploadImageAndSave(
            String name,
            String profession,
            String description,
            String priceFormatted,
            String priceType,
            String country,
            String city
    ) {

        if (imageUri == null) {

            saveServiceToFirestore(
                    name,
                    profession,
                    description,
                    priceFormatted,
                    priceType,
                    existingImageUrl,
                    country,
                    city
            );

            return;
        }

        if (!isValidImage(imageUri)) {

            setSubmittingState(false);

            return;
        }

        String extension =
                getFileExtension(imageUri);

        if (extension == null ||
                extension.trim().isEmpty()) {

            showError(
                    "Unsupported image format"
            );

            return;
        }

        String fileName =
                UUID.randomUUID()
                        .toString()
                        + "."
                        + extension;

        StorageReference fileRef =
                storageRef.child(
                        "service_images/"
                                + fileName
                );

        String mimeType =
                getContentResolver()
                        .getType(imageUri);

        StorageMetadata metadata =
                new StorageMetadata.Builder()
                        .setContentType(mimeType)
                        .build();

        fileRef.putFile(
                        imageUri,
                        metadata
                )
                .addOnSuccessListener(
                        taskSnapshot ->
                                fileRef.getDownloadUrl()
                                        .addOnSuccessListener(
                                                downloadUri ->
                                                        saveServiceToFirestore(
                                                                name,
                                                                profession,
                                                                description,
                                                                priceFormatted,
                                                                priceType,
                                                                downloadUri.toString(),
                                                                country,
                                                                city
                                                        )
                                        )
                                        .addOnFailureListener(
                                                e ->
                                                        showError(
                                                                "Could not get uploaded image URL"
                                                        )
                                        )
                )
                .addOnFailureListener(
                        e ->
                                showError(
                                        "Image upload failed"
                                )
                );
    }

    private void saveServiceToFirestore(
            String name,
            String profession,
            String description,
            String priceFormatted,
            String priceType,
            @Nullable String imageUrl,
            String country,
            String city
    ) {

        List<String> tags =
                generateTags(
                        profession,
                        description
                );

        Map<String, Object> service =
                new HashMap<>();

        service.put(
                "name",
                name
        );

        service.put(
                "profession",
                profession
        );

        service.put(
                "description",
                description
        );

        service.put(
                "price",
                priceFormatted
        );

        service.put(
                "priceType",
                priceType
        );

        service.put(
                "userId",
                userId
        );

        service.put(
                "tags",
                tags
        );

        if (imageUrl != null &&
                !imageUrl.trim().isEmpty()) {

            service.put(
                    "imageUrl",
                    imageUrl
            );
        }

        if (country != null &&
                !country.isEmpty()) {

            service.put(
                    "country",
                    country
            );
        }

        if (city != null &&
                !city.isEmpty()) {

            service.put(
                    "city",
                    city
            );
        }

        if (isEditMode &&
                editServiceId != null) {

            service.put(
                    "updatedAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("services")
                    .document(editServiceId)
                    .update(service)
                    .addOnSuccessListener(
                            unused -> {

                                Toast.makeText(
                                        this,
                                        "Service updated!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                navigateHome();
                            }
                    )
                    .addOnFailureListener(
                            e ->
                                    showError(
                                            "Could not update service"
                                    )
                    );

        } else {

            service.put(
                    "rating",
                    0.0
            );

            service.put(
                    "ratingCount",
                    0
            );

            service.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("services")
                    .add(service)
                    .addOnSuccessListener(
                            documentReference -> {

                                Toast.makeText(
                                        this,
                                        "Service added!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                navigateHome();
                            }
                    )
                    .addOnFailureListener(
                            e ->
                                    showError(
                                            "Could not save service"
                                    )
                    );
        }
    }

    private List<String> generateTags(
            String profession,
            String description
    ) {

        Set<String> uniqueTags =
                new HashSet<>();

        String normalizedProfession =
                profession
                        .toLowerCase(Locale.ROOT)
                        .trim();

        uniqueTags.add(
                normalizedProfession
        );

        String normalizedDescription =
                description
                        .toLowerCase(Locale.ROOT);

        String[] words =
                normalizedDescription.split(
                        "[\\s,.?!:;()\\[\\]{}+/\\-]+"
                );

        for (String word : words) {

            String cleanWord =
                    word.trim();

            if (cleanWord.length() > 3 &&
                    !STOP_WORDS.contains(cleanWord)) {

                uniqueTags.add(cleanWord);
            }
        }

        if (normalizedProfession.contains("php")) {

            uniqueTags.addAll(
                    Arrays.asList(
                            "backend",
                            "server",
                            "web",
                            "database"
                    )
            );

        } else if (
                normalizedProfession.contains("ios") ||
                        normalizedProfession.contains("swift")
        ) {

            uniqueTags.addAll(
                    Arrays.asList(
                            "mobile",
                            "apple",
                            "swift",
                            "iphone"
                    )
            );

        } else if (
                normalizedProfession.contains("android")
        ) {

            uniqueTags.addAll(
                    Arrays.asList(
                            "mobile",
                            "java",
                            "kotlin",
                            "google"
                    )
            );

        } else if (
                normalizedProfession.contains("electrician")
        ) {

            uniqueTags.addAll(
                    Arrays.asList(
                            "electrical",
                            "wiring",
                            "repair",
                            "maintenance"
                    )
            );

        } else if (
                normalizedProfession.contains("plumber")
        ) {

            uniqueTags.addAll(
                    Arrays.asList(
                            "pipe",
                            "leak",
                            "water",
                            "repair"
                    )
            );

        } else if (
                normalizedProfession.contains("developer") ||
                        normalizedProfession.contains("programmer")
        ) {

            uniqueTags.addAll(
                    Arrays.asList(
                            "coding",
                            "software",
                            "development"
                    )
            );
        }

        return new ArrayList<>(uniqueTags);
    }

    private boolean containsForbiddenWord(
            String text
    ) {

        if (text == null ||
                text.trim().isEmpty()) {

            return false;
        }

        String normalized =
                text.toLowerCase(Locale.ROOT)
                        .replaceAll(
                                "[^\\p{L}\\p{N}]+",
                                " "
                        )
                        .trim();

        if (normalized.isEmpty()) {
            return false;
        }

        String[] words =
                normalized.split("\\s+");

        for (String word : words) {

            for (String forbidden :
                    FORBIDDEN_WORDS) {

                if (word.equals(forbidden)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalizeText(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizeLocation(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String getFileExtension(
            Uri uri
    ) {

        if (uri == null) {
            return null;
        }

        String mimeType =
                getContentResolver()
                        .getType(uri);

        if (mimeType == null) {
            return null;
        }

        String extension =
                MimeTypeMap
                        .getSingleton()
                        .getExtensionFromMimeType(
                                mimeType
                        );

        if (extension == null) {
            return null;
        }

        return extension.toLowerCase(
                Locale.ROOT
        );
    }

    private void setSubmittingState(
            boolean submitting
    ) {

        isSubmitting = submitting;

        btnSubmit.setEnabled(!submitting);
        btnSelectImage.setEnabled(!submitting);

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

    private void showError(
            String message
    ) {

        setSubmittingState(false);

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

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        finish();
    }

    private void setupBottomNavigation() {

        bottomNavigationView
                .setOnNavigationItemSelectedListener(
                        item -> {

                            int id =
                                    item.getItemId();

                            if (id == R.id.nav_home) {

                                startActivity(
                                        new Intent(
                                                this,
                                                HomeActivity.class
                                        )
                                );

                                finish();

                                return true;

                            } else if (
                                    id == R.id.nav_chats
                            ) {

                                startActivity(
                                        new Intent(
                                                this,
                                                ChatListActivity.class
                                        )
                                );

                                finish();

                                return true;

                            } else if (
                                    id == R.id.nav_add
                            ) {

                                return true;

                            } else if (
                                    id == R.id.nav_profile
                            ) {

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

        bottomNavigationView
                .setSelectedItemId(
                        R.id.nav_add
                );
    }

    @Override
    public boolean onSupportNavigateUp() {

        getOnBackPressedDispatcher()
                .onBackPressed();

        return true;
    }
}