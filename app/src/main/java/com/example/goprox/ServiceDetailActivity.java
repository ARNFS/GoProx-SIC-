package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDetailActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvProfession, tvDescription, tvPrice, tvRatingText, tvRatingCount;
    private RatingBar ratingBar, ratingBarUser;
    private Button btnContact, btnMessage, btnSubmitReview;
    private EditText etReview;
    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;

    private FirebaseFirestore db;
    private String serviceId;
    private String otherUserId;
    private String serviceName;
    private float currentRating;
    private int ratingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        initViews();
        setupToolbar();
        loadDataFromIntent();
        loadReviews();
        setupRatingListener();
        setupReviewButton();
        setupButtons();
    }

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
        btnContact = findViewById(R.id.btnContact);
        btnMessage = findViewById(R.id.btnMessage);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        etReview = findViewById(R.id.etReview);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);

        db = FirebaseFirestore.getInstance();

        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(new ArrayList<>());
        recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadDataFromIntent() {
        serviceId = getIntent().getStringExtra("serviceId");
        String name = getIntent().getStringExtra("name");
        String profession = getIntent().getStringExtra("profession");
        String description = getIntent().getStringExtra("description");
        String price = getIntent().getStringExtra("price");
        float rating = getIntent().getFloatExtra("rating", 0);
        int ratingCountInt = getIntent().getIntExtra("ratingCount", 0);
        int imageResId = getIntent().getIntExtra("imageResId", R.drawable.ic_profile_placeholder);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        otherUserId = getIntent().getStringExtra("userId");
        serviceName = name;

        currentRating = rating;
        ratingCount = ratingCountInt;

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(imageResId)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(imageResId);
        }

        tvName.setText(name);
        tvProfession.setText(profession);
        tvDescription.setText(description);
        tvPrice.setText(price);
        ratingBar.setRating(rating);
        tvRatingText.setText(String.format("%.1f", rating));
        tvRatingCount.setText("(" + ratingCount + " reviews)");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
        }
    }

    private void loadReviews() {
        db.collection("services").document(serviceId)
                .collection("reviews")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userName = doc.getString("userName");
                        float userRating = doc.getDouble("rating") != null ? doc.getDouble("rating").floatValue() : 0;
                        String comment = doc.getString("comment");
                        long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                        reviews.add(new Review(userName, userRating, comment, timestamp));
                    }
                    reviewAdapter.updateList(reviews);
                });
    }

    private void setupRatingListener() {
        ratingBarUser.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                // Don't submit immediately, wait for review button
                Toast.makeText(this, "Rating: " + rating + " stars", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupReviewButton() {
        btnSubmitReview.setOnClickListener(v -> {
            float userRating = ratingBarUser.getRating();
            String comment = etReview.getText().toString().trim();

            if (userRating == 0) {
                Toast.makeText(this, "Please rate first", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (currentUserId == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUserId.equals(otherUserId)) {
                Toast.makeText(this, "You cannot rate yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            submitReview(currentUserId, userRating, comment);
        });
    }

    private void submitReview(String userId, float rating, String comment) {
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null) userName = "User";

        Map<String, Object> review = new HashMap<>();
        review.put("userId", userId);
        review.put("userName", userName);
        review.put("rating", rating);
        review.put("comment", comment);
        review.put("timestamp", System.currentTimeMillis());

        // Add review to subcollection
        db.collection("services").document(serviceId)
                .collection("reviews")
                .add(review)
                .addOnSuccessListener(docRef -> {
                    // Update service rating
                    updateServiceRating(rating);
                    etReview.setText("");
                    ratingBarUser.setRating(0);
                    Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateServiceRating(float newRating) {
        float newAverage = (currentRating * ratingCount + newRating) / (ratingCount + 1);
        int newCount = ratingCount + 1;

        db.collection("services").document(serviceId)
                .update("rating", newAverage, "ratingCount", newCount)
                .addOnSuccessListener(aVoid -> {
                    currentRating = newAverage;
                    ratingCount = newCount;
                    ratingBar.setRating(newAverage);
                    tvRatingText.setText(String.format("%.1f", newAverage));
                    tvRatingCount.setText("(" + newCount + " reviews)");
                });
    }

    private void setupButtons() {
        btnContact.setOnClickListener(v -> {
            Toast.makeText(this, "Calling " + serviceName + "...", Toast.LENGTH_SHORT).show();
        });

        btnMessage.setOnClickListener(v -> {
            if (otherUserId == null || otherUserId.isEmpty()) {
                Toast.makeText(this, "Cannot start chat: user ID missing", Toast.LENGTH_SHORT).show();
                return;
            }
            openChat();
        });
    }

    private void openChat() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(otherUserId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }

        Intent intent = new Intent(ServiceDetailActivity.this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserName", serviceName);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
//4