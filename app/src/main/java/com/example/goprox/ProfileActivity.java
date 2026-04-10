package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private CircleImageView ivProfileAvatar;          // исправлено
    private TextView tvName, tvEmail, tvMemberSince, tvNoPosts;
    private Button btnEditProfile, btnLogout, btnMyServices;
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
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);        // исправлено
        tvName = findViewById(R.id.tvProfileName);                   // исправлено
        tvEmail = findViewById(R.id.tvProfileEmail);                 // исправлено
        tvMemberSince = findViewById(R.id.tvMemberSince);            // добавлено (убедитесь, что есть в макете)
        tvNoPosts = findViewById(R.id.tvNoPosts);                    // если используется
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyServices = findViewById(R.id.btnMyServices);
        btnLogout = findViewById(R.id.btnLogout);
        rvMyPosts = findViewById(R.id.rvMyPosts);                    // если нужен список услуг
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

            // Заглушка аватарки
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void loadUserPosts() {
        myPostsList = new ArrayList<>();

        // TODO: загрузить реальные услуги пользователя из Firestore
        if (myPostsList.isEmpty()) {
            if (tvNoPosts != null) tvNoPosts.setVisibility(View.VISIBLE);
            if (rvMyPosts != null) rvMyPosts.setVisibility(View.GONE);
        } else {
            if (tvNoPosts != null) tvNoPosts.setVisibility(View.GONE);
            if (rvMyPosts != null) rvMyPosts.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        if (rvMyPosts == null) return;
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
        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(this, "Edit profile", Toast.LENGTH_SHORT).show()
        );
        btnMyServices.setOnClickListener(v ->
                Toast.makeText(this, "My services", Toast.LENGTH_SHORT).show()
        );
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