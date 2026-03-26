package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;

public class ServiceDetailActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvProfession, tvDescription, tvPrice, tvRatingText;
    private RatingBar ratingBar;
    private Button btnContact, btnMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        ivProfile = findViewById(R.id.ivDetailProfile);
        tvName = findViewById(R.id.tvDetailName);
        tvProfession = findViewById(R.id.tvDetailProfession);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvRatingText = findViewById(R.id.tvDetailRatingText);
        ratingBar = findViewById(R.id.ratingBarDetail);
        btnContact = findViewById(R.id.btnContact);
        btnMessage = findViewById(R.id.btnMessage);

        String name = getIntent().getStringExtra("name");
        if (name == null) name = "Unknown";

        String profession = getIntent().getStringExtra("profession");
        if (profession == null) profession = "Professional";

        String description = getIntent().getStringExtra("description");
        if (description == null) description = "No description available";

        String price = getIntent().getStringExtra("price");
        if (price == null) price = "$0";

        float rating = getIntent().getFloatExtra("rating", 0);
        int imageResId = getIntent().getIntExtra("imageResId", R.drawable.ic_profile_placeholder);
        String otherUserId = getIntent().getStringExtra("userId");
        if (otherUserId == null) otherUserId = "";

        final String finalName = name;
        final String finalOtherUserId = otherUserId;

        ivProfile.setImageResource(imageResId);
        tvName.setText(name);
        tvProfession.setText(profession);
        tvDescription.setText(description);
        tvPrice.setText(price);
        ratingBar.setRating(rating);
        tvRatingText.setText(String.valueOf(rating));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(name);
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        btnContact.setOnClickListener(v -> {
            Toast.makeText(this, "Calling " + finalName + "...", Toast.LENGTH_SHORT).show();
        });

        btnMessage.setOnClickListener(v -> {
            if (finalOtherUserId.isEmpty()) {
                Toast.makeText(this, "Cannot start chat: user ID missing", Toast.LENGTH_SHORT).show();
                return;
            }
            openChat(finalOtherUserId, finalName);
        });
    }

    private void openChat(String otherUserId, String otherUserName) {
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
        intent.putExtra("otherUserName", otherUserName);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}