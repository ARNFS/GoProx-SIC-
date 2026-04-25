package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
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
    private Button btnContact, btnMessage, btnSubmitReview;
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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
        btnContact = findViewById(R.id.btnContact);
        btnMessage = findViewById(R.id.btnMessage);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        etReview = findViewById(R.id.etReview);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);

        ratingBarUser.setStepSize(0.5f);
        ratingBarUser.setNumStars(5);

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
        int ratingCount = getIntent().getIntExtra("ratingCount", 0);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        otherUserId = getIntent().getStringExtra("userId");
        serviceName = name;

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(ivProfile);
        }
        tvName.setText(name != null ? name : "");
        tvProfession.setText(profession != null ? profession : "");
        tvDescription.setText(description != null ? description : "");
        tvPrice.setText(price != null ? price : "");

        ratingBar.setRating(rating);
        tvRatingText.setText(String.format("%.1f", rating));
        tvRatingCount.setText("(" + ratingCount + " reviews)");

        tvProfession.setTextColor(getResources().getColor(R.color.blue, getTheme()));

        if (otherUserId == null || otherUserId.isEmpty()) {
            loadUserIdFromFirestore();
        }
    }

    private void loadUserIdFromFirestore() {
        if (serviceId == null) return;
        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        otherUserId = doc.getString("userId");
                        if (otherUserId == null || otherUserId.isEmpty()) {
                            Toast.makeText(this, "Error: specialist ID missing", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load user", Toast.LENGTH_SHORT).show());
    }

    private void loadReviews() {
        if (serviceId == null) return;
        db.collection("services").document(serviceId).collection("reviews")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Review> reviews = new ArrayList<>();
                    for (var doc : snapshots) {
                        String userName = doc.getString("userName");
                        float userRating = doc.getDouble("rating") != null ? doc.getDouble("rating").floatValue() : 0;
                        String comment = doc.getString("comment");
                        long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                        reviews.add(new Review(userName, userRating, comment, timestamp));
                    }
                    reviewAdapter.updateList(reviews);
                });
    }

    private void setupReviewButton() {
        btnSubmitReview.setOnClickListener(v -> {
            String text = etReview.getText().toString().trim();
            float userRating = ratingBarUser.getRating();
            if (text.isEmpty() || userRating == 0) {
                Toast.makeText(this, "Please enter a review and rating", Toast.LENGTH_SHORT).show();
                return;
            }
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (userName == null) userName = "User";

            Map<String, Object> review = new HashMap<>();
            review.put("userId", currentUserId);
            review.put("userName", userName);
            review.put("rating", userRating);
            review.put("comment", text);
            review.put("timestamp", System.currentTimeMillis());

            db.collection("services").document(serviceId).collection("reviews")
                    .add(review)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Review added", Toast.LENGTH_SHORT).show();
                        etReview.setText("");
                        ratingBarUser.setRating(0);
                        updateServiceRating();
                        loadReviews();
                    });
        });
    }

    private void updateServiceRating() {
        db.collection("services").document(serviceId).collection("reviews")
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

                    ratingBar.setRating(newAvg);
                    tvRatingText.setText(String.format("%.1f", newAvg));
                    tvRatingCount.setText("(" + count + " reviews)");
                });
    }

    private void setupButtons() {
        btnContact.setOnClickListener(v -> {
            if (otherUserId == null || otherUserId.isEmpty()) {
                Toast.makeText(this, "Loading specialist info...", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Call type")
                    .setItems(new String[]{"Audio", "Video"}, (dialog, which) -> {
                        String channelName = (currentUserId.compareTo(otherUserId) < 0) ?
                                currentUserId + "_" + otherUserId : otherUserId + "_" + currentUserId;

                        Intent i = new Intent(ServiceDetailActivity.this, CallActivity.class);
                        i.putExtra("channelName", channelName);
                        i.putExtra("IS_AUDIO_ONLY", which == 0);
                        startActivity(i);
                    })
                    .show();
        });

        btnMessage.setOnClickListener(v -> openChat());
    }

    private void openChat() {
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Wait, loading specialist...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId.equals(otherUserId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserName", tvName.getText().toString());
        startActivity(intent);
    }
}