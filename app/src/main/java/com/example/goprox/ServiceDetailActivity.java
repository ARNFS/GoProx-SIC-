package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDetailActivity extends BaseActivity {

    private ImageView ivProfile;
    private TextView tvName, tvProfession, tvDescription, tvPrice;
    private TextView tvRatingText, tvRatingCount;

    private RatingBar ratingBar, ratingBarUser;

    private Button btnRequestService;
    private Button btnContact, btnMessage, btnSubmitReview;

    private EditText etReview;

    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;

    private FirebaseFirestore db;
    private ServiceRequestRepository serviceRequestRepository;

    private String serviceId;
    private String otherUserId;
    private String serviceName;
    private String servicePrice;
    private String servicePriceType;
    private String currentUserId;

    private boolean hasAlreadyReviewed = false;
    private boolean requestSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        db = FirebaseFirestore.getInstance();
        serviceRequestRepository = new ServiceRequestRepository();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please sign in",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        currentUserId = user.getUid();

        initViews();
        setupToolbar();
        loadDataFromIntent();
        setupButtons();
        setupRequestButton();
        setupReviewButton();
        checkIfAlreadyReviewed();
        loadReviews();
    }

    // =========================================================
    // INIT VIEWS
    // =========================================================

    private void initViews() {

        ivProfile = findViewById(R.id.ivDetailProfile);

        tvName = findViewById(R.id.tvDetailName);
        tvProfession = findViewById(R.id.tvDetailProfession);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvPrice = findViewById(R.id.tvDetailPrice);

        tvRatingText = findViewById(R.id.tvDetailRatingText);
        tvRatingCount = findViewById(R.id.tvRatingCount);

        ratingBar = findViewById(R.id.ratingBarDetail);
        ratingBarUser = findViewById(R.id.ratingBarUser);

        btnRequestService = findViewById(R.id.btnRequestService);
        btnContact = findViewById(R.id.btnContact);
        btnMessage = findViewById(R.id.btnMessage);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        etReview = findViewById(R.id.etReview);

        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);

        if (recyclerViewReviews == null
                || btnSubmitReview == null
                || btnMessage == null
                || btnContact == null
                || btnRequestService == null) {

            Toast.makeText(
                    this,
                    "UI initialization error",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        if (ratingBarUser != null) {
            ratingBarUser.setStepSize(0.5f);
            ratingBarUser.setNumStars(5);
        }

        recyclerViewReviews.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerViewReviews.setNestedScrollingEnabled(true);

        reviewAdapter = new ReviewAdapter(
                new ArrayList<>()
        );

        recyclerViewReviews.setAdapter(reviewAdapter);
    }

    // =========================================================
    // TOOLBAR
    // =========================================================

    private void setupToolbar() {

        Toolbar toolbar = findViewById(R.id.toolbar);

        if (toolbar != null) {

            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar()
                        .setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    // =========================================================
    // LOAD SERVICE DATA
    // =========================================================

    private void loadDataFromIntent() {

        Intent intent = getIntent();

        serviceId = intent.getStringExtra("serviceId");

        String name =
                intent.getStringExtra("name");

        String profession =
                intent.getStringExtra("profession");

        String description =
                intent.getStringExtra("description");

        String price =
                intent.getStringExtra("price");

        servicePrice = price;

        servicePriceType =
                intent.getStringExtra("priceType");

        float rating =
                intent.getFloatExtra("rating", 0);

        int ratingCount =
                intent.getIntExtra("ratingCount", 0);

        String imageUrl =
                intent.getStringExtra("imageUrl");

        otherUserId =
                intent.getStringExtra("userId");

        serviceName = name;

        // -----------------------------------------------------
        // IMAGE
        // -----------------------------------------------------

        if (ivProfile != null
                && imageUrl != null
                && !imageUrl.isEmpty()) {

            try {

                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(
                                R.drawable.ic_profile_placeholder
                        )
                        .error(
                                R.drawable.ic_profile_placeholder
                        )
                        .into(ivProfile);

            } catch (Exception ignored) {
            }
        }

        // -----------------------------------------------------
        // TEXT
        // -----------------------------------------------------

        if (tvName != null) {
            tvName.setText(
                    name != null ? name : ""
            );
        }

        if (tvProfession != null) {

            tvProfession.setText(
                    profession != null
                            ? profession
                            : ""
            );

            try {

                tvProfession.setTextColor(
                        getResources().getColor(
                                R.color.blue,
                                getTheme()
                        )
                );

            } catch (Exception ignored) {
            }
        }

        if (tvDescription != null) {

            String desc =
                    description != null
                            ? description
                            : "";

            tvDescription.setLinksClickable(true);

            tvDescription.setMovementMethod(
                    LinkMovementMethod.getInstance()
            );

            tvDescription.setText(
                    Html.fromHtml(
                            desc,
                            Html.FROM_HTML_MODE_LEGACY
                    )
            );
        }

        if (tvPrice != null) {
            tvPrice.setText(
                    price != null ? price : ""
            );
        }

        if (ratingBar != null) {
            ratingBar.setRating(rating);
        }

        if (tvRatingText != null) {

            tvRatingText.setText(
                    String.format(
                            "%.1f",
                            rating
                    )
            );
        }

        if (tvRatingCount != null) {

            tvRatingCount.setText(
                    "(" + ratingCount + " reviews)"
            );
        }

        // -----------------------------------------------------
        // FALLBACK: LOAD USER ID FROM FIRESTORE
        // -----------------------------------------------------

        if (otherUserId == null
                || otherUserId.isEmpty()) {

            loadUserIdFromFirestore();
        }

        // -----------------------------------------------------
        // OWN SERVICE
        // -----------------------------------------------------

        updateRequestButtonVisibility();
    }

    // =========================================================
    // LOAD USER ID
    // =========================================================

    private void loadUserIdFromFirestore() {

        if (serviceId == null
                || serviceId.isEmpty()) {
            return;
        }

        db.collection("services")
                .document(serviceId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc != null
                            && doc.exists()) {

                        otherUserId =
                                doc.getString("userId");

                        if (otherUserId == null
                                || otherUserId.isEmpty()) {

                            Toast.makeText(
                                    this,
                                    "Error: specialist ID missing",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        updateRequestButtonVisibility();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load user",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    // =========================================================
    // REQUEST BUTTON VISIBILITY
    // =========================================================

    private void updateRequestButtonVisibility() {

        if (btnRequestService == null) {
            return;
        }

        if (otherUserId != null
                && otherUserId.equals(currentUserId)) {

            btnRequestService.setVisibility(
                    View.GONE
            );

        } else {

            btnRequestService.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        if (btnContact != null) {

            btnContact.setOnClickListener(v -> {

                if (otherUserId == null
                        || otherUserId.isEmpty()) {

                    Toast.makeText(
                            this,
                            "Loading specialist info...",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                String title =
                        tvProfession != null
                                ? tvProfession
                                  .getText()
                                  .toString()
                                : "";

                CallHelper.startCall(
                        this,
                        otherUserId,
                        tvName != null
                                ? tvName
                                  .getText()
                                  .toString()
                                : "User",
                        title
                );
            });
        }

        if (btnMessage != null) {
            btnMessage.setOnClickListener(
                    v -> openChat()
            );
        }
    }

    // =========================================================
    // REQUEST SERVICE
    // =========================================================

    private void setupRequestButton() {

        if (btnRequestService == null) {
            return;
        }

        btnRequestService.setOnClickListener(
                v -> showRequestServiceDialog()
        );
    }

    private void showRequestServiceDialog() {

        if (requestSubmitting) {
            return;
        }

        if (serviceId == null
                || serviceId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Service information is missing",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (otherUserId == null
                || otherUserId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Loading specialist information...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (otherUserId.equals(currentUserId)) {

            Toast.makeText(
                    this,
                    "You cannot request your own service",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        int horizontalPadding =
                dpToPx(24);

        int verticalPadding =
                dpToPx(8);

        container.setPadding(
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                verticalPadding
        );

        EditText etMessage =
                new EditText(this);

        etMessage.setHint(
                "Describe what you need (optional)"
        );

        etMessage.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        etMessage.setMinLines(3);
        etMessage.setMaxLines(5);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        container.addView(
                etMessage,
                messageParams
        );

        EditText etRequestedPrice =
                new EditText(this);

        etRequestedPrice.setHint(
                "Requested price"
        );

        etRequestedPrice.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        if (servicePrice != null
                && !servicePrice.trim().isEmpty()) {

            etRequestedPrice.setText(
                    servicePrice
            );

            etRequestedPrice.setSelection(
                    etRequestedPrice
                            .getText()
                            .length()
            );
        }

        LinearLayout.LayoutParams priceParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        priceParams.topMargin =
                dpToPx(12);

        container.addView(
                etRequestedPrice,
                priceParams
        );

        String title =
                serviceName != null
                        && !serviceName.trim().isEmpty()
                        ? serviceName
                        : "this service";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Request " + title
                        )
                        .setView(container)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Send Request",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored -> {

                    Button positiveButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    positiveButton.setOnClickListener(
                            view -> {

                                String message =
                                        etMessage
                                                .getText()
                                                .toString()
                                                .trim();

                                String requestedPrice =
                                        etRequestedPrice
                                                .getText()
                                                .toString()
                                                .trim();

                                if (requestedPrice.isEmpty()) {

                                    etRequestedPrice.setError(
                                            "Enter a price"
                                    );

                                    return;
                                }

                                submitServiceRequest(
                                        dialog,
                                        message,
                                        requestedPrice
                                );
                            }
                    );
                }
        );

        dialog.show();
    }

    private void submitServiceRequest(
            AlertDialog dialog,
            String message,
            String requestedPrice
    ) {

        if (requestSubmitting) {
            return;
        }

        requestSubmitting = true;

        if (btnRequestService != null) {

            btnRequestService.setEnabled(false);

            btnRequestService.setText(
                    "Sending..."
            );
        }

        serviceRequestRepository
                .createRequest(
                        serviceId,
                        message,
                        requestedPrice,
                        servicePriceType != null
                                ? servicePriceType
                                : ""
                )
                .addOnSuccessListener(
                        requestReference -> {

                            requestSubmitting = false;

                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }

                            if (btnRequestService != null) {

                                btnRequestService.setEnabled(
                                        false
                                );

                                btnRequestService.setText(
                                        "Request Sent"
                                );
                            }

                            Toast.makeText(
                                    this,
                                    "Service request sent",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            requestSubmitting = false;

                            if (btnRequestService != null) {

                                btnRequestService.setEnabled(
                                        true
                                );

                                btnRequestService.setText(
                                        "Request Service"
                                );
                            }

                            Toast.makeText(
                                    this,
                                    getRequestErrorMessage(e),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private String getRequestErrorMessage(
            Exception exception
    ) {

        if (exception == null) {
            return "Failed to send request";
        }

        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return "Failed to send request";
        }

        if (message.contains(
                "PERMISSION_DENIED"
        )) {

            return "Request was denied by security rules";
        }

        if (message.contains(
                "does not exist"
        )) {

            return "This service no longer exists";
        }

        if (message.contains(
                "own service"
        )) {

            return "You cannot request your own service";
        }

        return "Failed to send request";
    }

    // =========================================================
    // CHAT
    // =========================================================

    private void openChat() {

        if (otherUserId == null
                || otherUserId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Wait, loading specialist...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (otherUserId.equals(currentUserId)) {

            Toast.makeText(
                    this,
                    "You cannot chat with yourself",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ChatActivity.class
                );

        intent.putExtra(
                "otherUserId",
                otherUserId
        );

        intent.putExtra(
                "otherUserName",
                tvName != null
                        ? tvName.getText().toString()
                        : "Unknown"
        );

        startActivity(intent);
    }

    // =========================================================
    // REVIEWS
    // =========================================================

    private void checkIfAlreadyReviewed() {

        if (serviceId == null
                || currentUserId == null) {
            return;
        }

        db.collection("services")
                .document(serviceId)
                .collection("reviews")
                .whereEqualTo(
                        "userId",
                        currentUserId
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            if (!snapshots.isEmpty()) {

                                hasAlreadyReviewed = true;

                                if (btnSubmitReview != null) {

                                    btnSubmitReview
                                            .setEnabled(false);

                                    btnSubmitReview
                                            .setText(
                                                    "Already Reviewed"
                                            );
                                }

                                if (ratingBarUser != null) {
                                    ratingBarUser
                                            .setIsIndicator(true);
                                }

                                if (etReview != null) {

                                    etReview.setEnabled(
                                            false
                                    );

                                    etReview.setHint(
                                            "You have already reviewed"
                                    );
                                }
                            }
                        }
                );
    }

    private void setupReviewButton() {

        if (btnSubmitReview == null) {
            return;
        }

        btnSubmitReview.setOnClickListener(
                v -> {

                    if (hasAlreadyReviewed) {

                        Toast.makeText(
                                this,
                                "You have already reviewed this service",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    float userRating =
                            ratingBarUser != null
                                    ? ratingBarUser.getRating()
                                    : 0;

                    String text =
                            etReview != null
                                    ? etReview
                                      .getText()
                                      .toString()
                                      .trim()
                                    : "";

                    if (userRating == 0) {

                        Toast.makeText(
                                this,
                                "Please select a rating",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String userName =
                            FirebaseAuth
                                    .getInstance()
                                    .getCurrentUser() != null
                                    ? FirebaseAuth
                                      .getInstance()
                                      .getCurrentUser()
                                      .getDisplayName()
                                    : null;

                    if (userName == null) {
                        userName = "User";
                    }

                    Map<String, Object> review =
                            new HashMap<>();

                    review.put(
                            "userId",
                            currentUserId
                    );

                    review.put(
                            "userName",
                            userName
                    );

                    review.put(
                            "rating",
                            userRating
                    );

                    review.put(
                            "comment",
                            text
                    );

                    review.put(
                            "timestamp",
                            System.currentTimeMillis()
                    );

                    btnSubmitReview.setEnabled(false);

                    btnSubmitReview.setText(
                            "Submitting..."
                    );

                    db.collection("services")
                            .document(serviceId)
                            .collection("reviews")
                            .add(review)
                            .addOnSuccessListener(
                                    doc -> {

                                        hasAlreadyReviewed = true;

                                        Toast.makeText(
                                                this,
                                                "Review added",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        if (etReview != null) {
                                            etReview.setText("");
                                        }

                                        if (ratingBarUser != null) {
                                            ratingBarUser
                                                    .setRating(0);
                                        }

                                        btnSubmitReview
                                                .setText(
                                                        "Already Reviewed"
                                                );

                                        updateServiceRating();
                                        loadReviews();
                                    }
                            )
                            .addOnFailureListener(
                                    e -> {

                                        btnSubmitReview
                                                .setEnabled(true);

                                        btnSubmitReview
                                                .setText(
                                                        "Submit Review"
                                                );

                                        Toast.makeText(
                                                this,
                                                "Failed to add review",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                            );
                }
        );
    }

    private void loadReviews() {

        if (serviceId == null
                || reviewAdapter == null) {
            return;
        }

        db.collection("services")
                .document(serviceId)
                .collection("reviews")
                .orderBy(
                        "timestamp",
                        com.google.firebase.firestore.Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<Review> reviews =
                                    new ArrayList<>();

                            for (var doc : snapshots) {

                                String userName =
                                        doc.getString(
                                                "userName"
                                        );

                                float userRating = 0;

                                Double ratingDouble =
                                        doc.getDouble(
                                                "rating"
                                        );

                                if (ratingDouble != null) {
                                    userRating =
                                            ratingDouble.floatValue();
                                }

                                String comment =
                                        doc.getString(
                                                "comment"
                                        );

                                long timestamp = 0;

                                Long tsLong =
                                        doc.getLong(
                                                "timestamp"
                                        );

                                if (tsLong != null) {
                                    timestamp = tsLong;
                                }

                                reviews.add(
                                        new Review(
                                                userName != null
                                                        ? userName
                                                        : "Unknown",
                                                userRating,
                                                comment != null
                                                        ? comment
                                                        : "",
                                                timestamp
                                        )
                                );
                            }

                            reviewAdapter.updateList(
                                    reviews
                            );
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Failed to load reviews",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void updateServiceRating() {

        if (serviceId == null) {
            return;
        }

        db.collection("services")
                .document(serviceId)
                .collection("reviews")
                .get()
                .addOnSuccessListener(
                        docs -> {

                            float total = 0;
                            int count = docs.size();

                            for (var doc : docs) {

                                Double r =
                                        doc.getDouble(
                                                "rating"
                                        );

                                if (r != null) {
                                    total += r;
                                }
                            }

                            float newAvg =
                                    count > 0
                                            ? total / count
                                            : 0;

                            Map<String, Object> updates =
                                    new HashMap<>();

                            updates.put(
                                    "rating",
                                    newAvg
                            );

                            updates.put(
                                    "ratingCount",
                                    count
                            );

                            db.collection("services")
                                    .document(serviceId)
                                    .update(updates);

                            if (ratingBar != null) {
                                ratingBar.setRating(
                                        newAvg
                                );
                            }

                            if (tvRatingText != null) {

                                tvRatingText.setText(
                                        String.format(
                                                "%.1f",
                                                newAvg
                                        )
                                );
                            }

                            if (tvRatingCount != null) {

                                tvRatingCount.setText(
                                        "("
                                                + count
                                                + " reviews)"
                                );
                            }
                        }
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private int dpToPx(int dp) {

        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // =========================================================
    // BACK
    // =========================================================

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();

        return true;
    }
}