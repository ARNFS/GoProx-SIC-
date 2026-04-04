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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
            Glide.with(this).load(imageUrl).placeholder(imageResId).into(ivProfile);
        } else {
            ivProfile.setImageResource(imageResId);
        }

        tvName.setText(name != null ? name : "");
        tvProfession.setText(profession != null ? profession : "");
        tvDescription.setText(description != null ? description : "");
        tvPrice.setText(price != null ? price : "");
        ratingBar.setRating(rating);
        tvRatingText.setText(String.format("%.1f", rating));
        tvRatingCount.setText("(" + ratingCount + " reviews)");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name != null ? name : "Service");
        }
    }

    private void loadReviews() {
        if (serviceId == null) return;
        db.collection("services").document(serviceId)
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
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
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show());
    }

    private void setupRatingListener() {
        ratingBarUser.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) Toast.makeText(this, "Rating: " + rating, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupReviewButton() {
        btnSubmitReview.setOnClickListener(v -> {
            // review կոդը (կարող ես հետո ավելացնել, հիմա թողնում եմ դատարկ)
        });
    }

    private void setupButtons() {
        // ՏԵՍԱԶԱՆԳ (WebRTC)
        btnContact.setOnClickListener(v -> {
            if (otherUserId == null || otherUserId.trim().isEmpty()) {
                Toast.makeText(this, "Մասնագետի ID-ն չի գտնվել", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Տեսազանգը բացվում է...", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("SPECIALIST_ID", otherUserId);
            intent.putExtra("IS_CALLER", true);
            startActivity(intent);
        });

        // Հաղորդագրություն
        btnMessage.setOnClickListener(v -> openChat());
    }

    private void openChat() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null || otherUserId == null) {
            Toast.makeText(this, "Cannot start chat", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId.equals(otherUserId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = currentUserId.compareTo(otherUserId) < 0 ?
                currentUserId + "_" + otherUserId : otherUserId + "_" + currentUserId;

        Intent intent = new Intent(this, ChatActivity.class);
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