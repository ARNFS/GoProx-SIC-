package com.example.goprox;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private CircleImageView ivProfileAvatar;
    private TextView tvName, tvEmail, tvMemberSince, tvNoPosts;
    private Button btnEditProfile, btnLogout;
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

        if (ivProfileAvatar != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivProfileAvatar);
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

        // Click
        postsAdapter.setOnItemClickListener(position -> {
            if (position >= 0 && position < myPostsList.size()) {
                Service service = myPostsList.get(position);
                if (service != null) {
                    // 🔥 Edit via AddPostActivity
                    openEditService(service);
                }
            }
        });

        // Long Click — Delete
        postsAdapter.setOnItemLongClickListener(position -> {
            if (position >= 0 && position < myPostsList.size()) {
                Service service = myPostsList.get(position);
                if (service != null) {
                    showDeleteConfirmation(service, position);
                }
            }
        });
    }

    // 🔥 OPEN EDIT
    private void openEditService(Service service) {
        Intent intent = new Intent(this, AddPostActivity.class);
        intent.putExtra("serviceId", service.getId());
        intent.putExtra("name", service.getName());
        intent.putExtra("profession", service.getProfession());
        intent.putExtra("description", service.getDescription());
        intent.putExtra("price", service.getPrice());
        intent.putExtra("country", service.getCountry());
        intent.putExtra("city", service.getCity());
        intent.putExtra("imageUrl", service.getImageUrl());
        startActivity(intent);
    }

    // 🔥 DELETE CONFIRMATION
    // 🔥 DELETE CONFIRMATION — ԴԵՂԻՆ WARNING ICON-ՈՎ
    private void showDeleteConfirmation(Service service, int position) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to delete \"" + service.getName() + "\"?")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Delete", (d, which) -> {
                    FirebaseFirestore.getInstance().collection("services").document(service.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                myPostsList.remove(position);
                                postsAdapter.notifyItemRemoved(position);
                                updatePostsView();
                                Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        try {
            int yellowColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(yellowColor);
        } catch (Exception ignored) {}
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, HomeActivity.class)); finish(); return true; }
            else if (id == R.id.nav_chats) { startActivity(new Intent(this, ChatListActivity.class)); finish(); return true; }
            else if (id == R.id.nav_add) { startActivity(new Intent(this, AddPostActivity.class)); finish(); return true; }
            else if (id == R.id.nav_profile) { return true; }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    private void setupButtons() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (d, which) -> { mAuth.signOut(); startActivity(new Intent(this, LoginActivity.class)); finish(); })
                .setNegativeButton("No", (d, which) -> d.dismiss())
                .setIcon(R.drawable.ic_warning)
                .create();
        dialog.show();
        try { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark)); } catch (Exception ignored) {}
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}