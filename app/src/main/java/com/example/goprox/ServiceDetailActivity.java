package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDetailActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvProfession, tvDescription, tvPrice;
    private TextView tvRatingText, tvRatingCount;
    private RatingBar ratingBar, ratingBarUser;
    private Button btnMessage, btnSubmitReview;
    private EditText etReview;
    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;

    private FirebaseFirestore db;
    private String serviceId, otherUserId, serviceName, currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        initViews();
        setupToolbar();
        loadDataFromIntent();
        setupButtons();
        setupReviewButton();
        loadReviews();
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
        btnMessage = findViewById(R.id.btnMessage);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        etReview = findViewById(R.id.etReview);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);

        if (recyclerViewReviews == null || btnSubmitReview == null || btnMessage == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (ratingBarUser != null) {
            ratingBarUser.setStepSize(0.5f);
            ratingBarUser.setNumStars(5);
        }

        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(new ArrayList<>());
        recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();
        serviceId = intent.getStringExtra("serviceId");
        String name = intent.getStringExtra("name");
        String profession = intent.getStringExtra("profession");
        String description = intent.getStringExtra("description");
        String price = intent.getStringExtra("price");
        float rating = intent.getFloatExtra("rating", 0);
        int ratingCount = intent.getIntExtra("ratingCount", 0);
        String imageUrl = intent.getStringExtra("imageUrl");
        otherUserId = intent.getStringExtra("userId");
        serviceName = name;

        if (ivProfile != null && imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Glide.with(this).load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivProfile);
            } catch (Exception ignored) {}
        }

        if (tvName != null) tvName.setText(name != null ? name : "");
        if (tvProfession != null) {
            tvProfession.setText(profession != null ? profession : "");
            try {
                tvProfession.setTextColor(getResources().getColor(R.color.blue, getTheme()));
            } catch (Exception ignored) {}
        }
        if (tvDescription != null) tvDescription.setText(description != null ? description : "");
        if (tvPrice != null) tvPrice.setText(price != null ? price : "");
        if (ratingBar != null) ratingBar.setRating(rating);
        if (tvRatingText != null) tvRatingText.setText(String.format("%.1f", rating));
        if (tvRatingCount != null) tvRatingCount.setText("(" + ratingCount + " reviews)");

        if (otherUserId == null || otherUserId.isEmpty()) {
            loadUserIdFromFirestore();
        }
    }

    private void loadUserIdFromFirestore() {
        if (serviceId == null) return;
        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        otherUserId = doc.getString("userId");
                        if (otherUserId == null || otherUserId.isEmpty()) {
                            Toast.makeText(this,
                                    "Error: specialist ID missing",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load user", Toast.LENGTH_SHORT).show());
    }

    private void setupButtons() {
        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> openChat());
        }
    }

    private void openChat() {
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Wait, loading specialist...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (otherUserId.equals(currentUserId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserName", tvName != null ? tvName.getText().toString() : "Unknown");
        startActivity(intent);
    }

    private void setupReviewButton() {
        if (btnSubmitReview == null) return;

        btnSubmitReview.setOnClickListener(v -> {
            if (etReview == null || ratingBarUser == null) return;

            String text = etReview.getText().toString().trim();
            float userRating = ratingBarUser.getRating();

            if (text.isEmpty() || userRating == 0) {
                Toast.makeText(this,
                        "Please enter a review and rating",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String userName = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
                    : null;
            if (userName == null) userName = "User";

            Map<String, Object> review = new HashMap<>();
            review.put("userId", currentUserId);
            review.put("userName", userName);
            review.put("rating", userRating);
            review.put("comment", text);
            review.put("timestamp", System.currentTimeMillis());

            db.collection("services").document(serviceId)
                    .collection("reviews")
                    .add(review)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Review added", Toast.LENGTH_SHORT).show();
                        etReview.setText("");
                        ratingBarUser.setRating(0);
                        updateServiceRating();
                        loadReviews();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add review", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadReviews() {
        if (serviceId == null || reviewAdapter == null) return;

        db.collection("services").document(serviceId)
                .collection("reviews")
                .orderBy("timestamp",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Review> reviews = new ArrayList<>();
                    for (var doc : snapshots) {
                        String userName = doc.getString("userName");
                        float userRating = 0;
                        Double ratingDouble = doc.getDouble("rating");
                        if (ratingDouble != null) userRating = ratingDouble.floatValue();
                        String comment = doc.getString("comment");
                        long timestamp = 0;
                        Long tsLong = doc.getLong("timestamp");
                        if (tsLong != null) timestamp = tsLong;
                        reviews.add(new Review(
                                userName != null ? userName : "Unknown",
                                userRating,
                                comment != null ? comment : "",
                                timestamp));
                    }
                    reviewAdapter.updateList(reviews);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateServiceRating() {
        if (serviceId == null) return;

        db.collection("services").document(serviceId)
                .collection("reviews")
                .get()
                .addOnSuccessListener(docs -> {
                    float total = 0;
                    int count = docs.size();
                    for (var doc : docs) {
                        Double r = doc.getDouble("rating");
                        if (r != null) total += r;
                    }
                    float newAvg = count > 0 ? total / count : 0;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("rating", newAvg);
                    updates.put("ratingCount", count);
                    db.collection("services").document(serviceId).update(updates);

                    if (ratingBar != null) ratingBar.setRating(newAvg);
                    if (tvRatingText != null)
                        tvRatingText.setText(String.format("%.1f", newAvg));
                    if (tvRatingCount != null)
                        tvRatingCount.setText("(" + count + " reviews)");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}