package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private CircleImageView ivProfileAvatar;
    private TextView tvName, tvEmail, tvMemberSince, tvNoPosts;
    private Button btnEditProfile, btnLogout, btnMyServices;
    private RecyclerView rvMyPosts;
    private BottomNavigationView bottomNavigationView;

    private ProfilePostsAdapter postsAdapter;
    private final List<Service> myPostsList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvNoPosts = findViewById(R.id.tvNoPosts);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyServices = findViewById(R.id.btnMyServices);
        btnLogout = findViewById(R.id.btnLogout);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        if (btnLogout == null || bottomNavigationView == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (toolbar != null) setSupportActionBar(toolbar);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadUserData() {
        if (currentUser == null) return;

        String name = currentUser.getDisplayName();
        String email = currentUser.getEmail();
        long creationTime = 0;
        try {
            if (currentUser.getMetadata() != null) {
                creationTime = currentUser.getMetadata().getCreationTimestamp();
            }
        } catch (Exception ignored) {}

        if (tvName != null) tvName.setText(name != null ? name : "User");
        if (tvEmail != null) tvEmail.setText(email != null ? email : "No email");

        if (tvMemberSince != null) {
            if (creationTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                String date = sdf.format(new Date(creationTime));
                tvMemberSince.setText("Member since " + date);
            } else {
                tvMemberSince.setText("Member");
            }
        }

        if (ivProfileAvatar != null) {
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void loadUserPosts() {
        myPostsList.clear();

        FirebaseService firebaseService = new FirebaseService();
        firebaseService.getAllServices(services -> {
            runOnUiThread(() -> {
                myPostsList.clear();
                if (services != null && currentUser != null) {
                    for (Service s : services) {
                        if (s != null && currentUser.getUid().equals(s.getUserId())) {
                            myPostsList.add(s);
                        }
                    }
                }
                updatePostsView();
            });
        });
    }

    private void updatePostsView() {
        if (myPostsList.isEmpty()) {
            if (tvNoPosts != null) tvNoPosts.setVisibility(View.VISIBLE);
            if (rvMyPosts != null) rvMyPosts.setVisibility(View.GONE);
        } else {
            if (tvNoPosts != null) tvNoPosts.setVisibility(View.GONE);
            if (rvMyPosts != null) rvMyPosts.setVisibility(View.VISIBLE);
        }
        if (postsAdapter != null) {
            postsAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        if (rvMyPosts == null) return;
        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new ProfilePostsAdapter(myPostsList);
        rvMyPosts.setAdapter(postsAdapter);

        postsAdapter.setOnItemClickListener(position -> {
            if (position >= 0 && position < myPostsList.size()) {
                Service service = myPostsList.get(position);
                if (service != null) {
                    Toast.makeText(this, "Edit post: " + service.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
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
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                    Toast.makeText(this, "Edit profile", Toast.LENGTH_SHORT).show());
        }
        if (btnMyServices != null) {
            btnMyServices.setOnClickListener(v ->
                    Toast.makeText(this, "My services", Toast.LENGTH_SHORT).show());
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}