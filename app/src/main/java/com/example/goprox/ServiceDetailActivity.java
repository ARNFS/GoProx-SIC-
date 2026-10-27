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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceDetailActivity extends BaseActivity {

    private ImageView ivProfile;
    private TextView tvName, tvProfession, tvDescription, tvPrice;
    private TextView tvRatingText, tvRatingCount;
    private TextView tvReviewEligibility;

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
    private boolean reviewEligibilityKnown = false;
    private boolean reviewAlreadyChecked = false;
    private boolean reviewHasCompletedRequest = false;
    private boolean reviewCheckFailed = false;
    private boolean reviewSubmitting = false;
    private String eligibleRequestId;
    private boolean serviceRatingVerified;

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
        checkReviewEligibility();
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
        tvReviewEligibility = findViewById(R.id.tvReviewEligibility);

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
        serviceRatingVerified = intent.getBooleanExtra("ratingVerified", false);

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
            String displayedPrice = price != null ? price : "";
            if (servicePriceType != null
                    && !servicePriceType.trim().isEmpty()) {
                displayedPrice += " / " + servicePriceType.trim();
            }
            tvPrice.setText(displayedPrice);
        }

        renderRating(rating, ratingCount, serviceRatingVerified);

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

                            GoProxAnalytics.logServiceRequestSubmitted(this);

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
                            hasAlreadyReviewed = !snapshots.isEmpty();
                            reviewAlreadyChecked = true;
                            applyReviewAccessState();
                        }
                )
                .addOnFailureListener(error -> {
                    reviewAlreadyChecked = false;
                    reviewCheckFailed = true;
                    applyReviewAccessState();
                });
    }

    private void checkReviewEligibility() {
        if (serviceId == null || currentUserId == null) return;

        reviewEligibilityKnown = false;
        applyReviewAccessState();
        db.collection("serviceRequests")
                .whereEqualTo("customerId", currentUserId)
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", ServiceRequest.STATUS_COMPLETED)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    reviewHasCompletedRequest = !snapshot.isEmpty();
                    eligibleRequestId = reviewHasCompletedRequest
                            ? snapshot.getDocuments().get(0).getId()
                            : null;
                    reviewEligibilityKnown = true;
                    applyReviewAccessState();
                })
                .addOnFailureListener(error -> {
                    reviewEligibilityKnown = false;
                    reviewCheckFailed = true;
                    applyReviewAccessState();
                });
    }

    private void applyReviewAccessState() {
        if (reviewCheckFailed) {
            if (tvReviewEligibility != null) {
                tvReviewEligibility.setText("Couldn't check review eligibility. Try again.");
            }
            if (btnSubmitReview != null) {
                btnSubmitReview.setText("Retry review check");
                btnSubmitReview.setEnabled(true);
            }
            if (ratingBarUser != null) ratingBarUser.setEnabled(false);
            if (etReview != null) etReview.setEnabled(false);
            return;
        }

        if (!reviewEligibilityKnown || !reviewAlreadyChecked) {
            if (tvReviewEligibility != null) {
                tvReviewEligibility.setText("Checking completed requests…");
            }
            if (btnSubmitReview != null) {
                btnSubmitReview.setText("Checking eligibility…");
                btnSubmitReview.setEnabled(false);
            }
            if (ratingBarUser != null) ratingBarUser.setEnabled(false);
            if (etReview != null) etReview.setEnabled(false);
            return;
        }

        if (hasAlreadyReviewed) {
            if (tvReviewEligibility != null) {
                tvReviewEligibility.setText("You've already reviewed this provider.");
            }
            if (btnSubmitReview != null) {
                btnSubmitReview.setText("Already reviewed");
                btnSubmitReview.setEnabled(false);
            }
            if (ratingBarUser != null) ratingBarUser.setEnabled(false);
            if (etReview != null) etReview.setEnabled(false);
            return;
        }

        boolean eligible = reviewHasCompletedRequest;
        if (tvReviewEligibility != null) {
            tvReviewEligibility.setText(eligible
                    ? "Your completed request makes you eligible to review."
                    : "Only customers with a completed GoProx request can leave a review.");
        }
        if (btnSubmitReview != null) {
            btnSubmitReview.setText(eligible ? "Submit review" : "Review after completion");
            btnSubmitReview.setEnabled(eligible && !reviewSubmitting);
        }
        if (ratingBarUser != null) ratingBarUser.setEnabled(eligible);
        if (etReview != null) etReview.setEnabled(eligible);
    }

    private void setupReviewButton() {

        if (btnSubmitReview == null) {
            return;
        }

        btnSubmitReview.setOnClickListener(
                v -> {

                    if (reviewCheckFailed) {
                        reviewCheckFailed = false;
                        reviewAlreadyChecked = false;
                        checkIfAlreadyReviewed();
                        checkReviewEligibility();
                        return;
                    }

                    if (!reviewEligibilityKnown || !reviewAlreadyChecked ||
                            !reviewHasCompletedRequest || hasAlreadyReviewed || reviewSubmitting) {

                        Toast.makeText(
                                this,
                                "A completed GoProx request is required to review this provider.",
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

                    if (eligibleRequestId == null || eligibleRequestId.trim().isEmpty()) {
                        Toast.makeText(this,
                                "Couldn't find your completed request. Refresh and try again.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    reviewSubmitting = true;
                    applyReviewAccessState();
                    Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("serviceId", serviceId);
                    payload.put("requestId", eligibleRequestId);
                    payload.put("rating", userRating);
                    payload.put("comment", text);
                    FirebaseFunctions.getInstance("europe-west1")
                            .getHttpsCallable("submitServiceReview")
                            .call(payload)
                            .addOnSuccessListener(result -> {
                                reviewSubmitting = false;
                                hasAlreadyReviewed = true;
                                if (etReview != null) etReview.setText("");
                                if (ratingBarUser != null) ratingBarUser.setRating(0);
                                applyReviewAccessState();
                                Toast.makeText(this, "Review added", Toast.LENGTH_SHORT).show();
                                loadReviews();
                                loadServiceRating();
                            })
                            .addOnFailureListener(error -> {
                                reviewSubmitting = false;
                                applyReviewAccessState();
                                String message = error instanceof FirebaseFunctionsException
                                        ? error.getMessage() : null;
                                Toast.makeText(this,
                                        message == null || message.trim().isEmpty()
                                                ? "Couldn't submit review. Try again."
                                                : message,
                                        Toast.LENGTH_LONG).show();
                            });
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
                            Set<String> seenReviewers = new HashSet<>();

                            for (var doc : snapshots) {

                                String reviewerId = doc.getString("userId");
                                if (reviewerId != null && !seenReviewers.add(reviewerId)) {
                                    continue;
                                }

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

    private void loadServiceRating() {

        if (serviceId == null) {
            return;
        }

        db.collection("services")
                .document(serviceId)
                .get()
                .addOnSuccessListener(document -> {
                    Double average = document.getDouble("rating");
                    Long count = document.getLong("ratingCount");
                    float rating = average == null ? 0f : average.floatValue();
                    int reviews = count == null ? 0 : count.intValue();
                    serviceRatingVerified = Boolean.TRUE.equals(document.getBoolean("ratingVerified"));
                    renderRating(rating, reviews, serviceRatingVerified);
                });
    }

    private void renderRating(float rating, int count, boolean verified) {
        boolean hasReviews = verified && count > 0;
        if (ratingBar != null) {
            ratingBar.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
            ratingBar.setRating(hasReviews ? rating : 0f);
        }
        if (tvRatingText != null) {
            tvRatingText.setText(!verified
                    ? "Older rating not verified"
                    : hasReviews
                    ? String.format(java.util.Locale.getDefault(), "%.1f", rating)
                    : "No reviews yet");
        }
        if (tvRatingCount != null) {
            tvRatingCount.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
            tvRatingCount.setText("(" + count + " reviews)");
        }
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
