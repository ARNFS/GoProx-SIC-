package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvName, tvEmail, tvMemberSince, tvNoPosts;
    private Button btnEditProfile, btnLogout;
    private RecyclerView rvMyPosts;
    private BottomNavigationView bottomNavigationView;

    private ProfilePostsAdapter postsAdapter;
    private List<Service> myPostsList;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupToolbar();
        loadUserData();
        loadUserPosts();
        setupRecyclerView();
        setupBottomNavigation();
        setupButtons();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvNoPosts = findViewById(R.id.tvNoPosts);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        setSupportActionBar(toolbar);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadUserData() {
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            long creationTime = currentUser.getMetadata() != null ?
                    currentUser.getMetadata().getCreationTimestamp() : 0;

            tvName.setText(name != null ? name : "User");
            tvEmail.setText(email != null ? email : "No email");

            if (creationTime > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy");
                String date = sdf.format(new java.util.Date(creationTime));
                tvMemberSince.setText("Member since " + date);
            } else {
                tvMemberSince.setText("Member");
            }

            ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void loadUserPosts() {
        myPostsList = new ArrayList<>();

        if (myPostsList.isEmpty()) {
            tvNoPosts.setVisibility(View.VISIBLE);
            rvMyPosts.setVisibility(View.GONE);
        } else {
            tvNoPosts.setVisibility(View.GONE);
            rvMyPosts.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new ProfilePostsAdapter(myPostsList);
        rvMyPosts.setAdapter(postsAdapter);

        postsAdapter.setOnItemClickListener(position -> {
            Service service = myPostsList.get(position);
            Toast.makeText(this, "Edit post: " + service.getName(), Toast.LENGTH_SHORT).show();
        });
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
                startActivity(new Intent(this, AddPostActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    private void setupButtons() {
        btnEditProfile.setOnClickListener(v -> Toast.makeText(this, "Edit profile", Toast.LENGTH_SHORT).show());
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}