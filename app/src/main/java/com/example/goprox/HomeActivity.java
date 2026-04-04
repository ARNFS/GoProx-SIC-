package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ServiceAdapter serviceAdapter;
    private BottomNavigationView bottomNavigationView;
    private TextView tvWelcome;
    private EditText etSearch;
    private ImageView ivAIToggle;
    private LinearLayout llNotFound;

    private List<Service> serviceList;
    private List<Service> originalServiceList;

    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        serviceList = new ArrayList<>();
        originalServiceList = new ArrayList<>();

        initViews();
        setWelcomeMessage();
        setupRecyclerView();

        firebaseService = new FirebaseService();
        loadServicesFromFirebase();

        setupAIToggle();
        setupSearch();
        setupBottomNavigation();
        setupItemClickListener();

        ArrayList<String> professionList = getIntent().getStringArrayListExtra("profession_filter_list");
        if (professionList != null && !professionList.isEmpty()) {
            filterByMultipleProfessions(professionList);
        } else {
            String singleFilter = getIntent().getStringExtra("profession_filter");
            if (singleFilter != null && !singleFilter.isEmpty()) {
                filterByProfession(singleFilter);
            }
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tvWelcome = findViewById(R.id.tvWelcome);
        etSearch = findViewById(R.id.etSearch);
        ivAIToggle = findViewById(R.id.ivAIToggle);
        llNotFound = findViewById(R.id.llNotFound);
    }

    private void setWelcomeMessage() {
        String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "User";
        tvWelcome.setText("Welcome back, " + userName + "!");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceAdapter(serviceList);
        recyclerView.setAdapter(serviceAdapter);
    }

    private void loadServicesFromFirebase() {
        firebaseService.getAllServices(services -> {
            serviceList.clear();
            serviceList.addAll(services);
            originalServiceList.clear();
            originalServiceList.addAll(services);
            Collections.sort(serviceList, (a, b) -> Float.compare(b.getRating(), a.getRating()));
            if (serviceAdapter != null) {
                serviceAdapter.notifyDataSetChanged();
            }
        });
    }

    private void filterByProfession(String query) {
        new Handler().postDelayed(() -> {
            List<Service> filtered = new ArrayList<>();
            String lowerQuery = query.toLowerCase();

            for (Service s : originalServiceList) {
                if (s.getProfession().toLowerCase().contains(lowerQuery)) {
                    filtered.add(s);
                    continue;
                }
                boolean tagMatch = false;
                if (s.getTags() != null) {
                    for (String tag : s.getTags()) {
                        if (tag.toLowerCase().contains(lowerQuery) || lowerQuery.contains(tag.toLowerCase())) {
                            tagMatch = true;
                            break;
                        }
                    }
                }
                if (tagMatch) {
                    filtered.add(s);
                    continue;
                }
                if (s.getDescription().toLowerCase().contains(lowerQuery)) {
                    filtered.add(s);
                }
            }

            if (filtered.isEmpty() && !lowerQuery.isEmpty()) {
                Toast.makeText(this, "No professionals found for: " + query, Toast.LENGTH_LONG).show();
                showResults(new ArrayList<>());
            } else {
                Collections.sort(filtered, (a, b) -> Float.compare(b.getRating(), a.getRating()));
                showResults(filtered);
                if (!filtered.isEmpty()) {
                    Toast.makeText(this, "Found " + filtered.size() + " matching professionals", Toast.LENGTH_SHORT).show();
                }
            }
        }, 500);
    }

    private void filterByMultipleProfessions(ArrayList<String> professions) {
        if (professions == null || professions.isEmpty()) return;

        new Handler().postDelayed(() -> {
            List<Service> filtered = new ArrayList<>();
            for (Service s : originalServiceList) {
                String profLower = s.getProfession().toLowerCase();
                boolean match = false;
                for (String p : professions) {
                    if (profLower.contains(p.toLowerCase())) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    filtered.add(s);
                }
            }

            if (filtered.isEmpty()) {
                Toast.makeText(this, "No professionals found for the requested types.", Toast.LENGTH_LONG).show();
                showResults(new ArrayList<>());
            } else {
                Collections.sort(filtered, (a, b) -> Float.compare(b.getRating(), a.getRating()));
                showResults(filtered);
                Toast.makeText(this, "Found " + filtered.size() + " matching professionals", Toast.LENGTH_SHORT).show();
            }
        }, 500);
    }

    private void setupAIToggle() {
        ivAIToggle.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AIDialogActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterServices(s.toString());
            }
        });
    }

    private void filterServices(String query) {
        List<Service> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(originalServiceList);
        } else {
            String q = query.toLowerCase();
            for (Service s : originalServiceList) {
                if (s.getName().toLowerCase().contains(q) ||
                        s.getProfession().toLowerCase().contains(q)) {
                    filtered.add(s);
                }
            }
        }
        showResults(filtered);
    }

    private void showResults(List<Service> results) {
        Collections.sort(results, (a, b) -> Float.compare(b.getRating(), a.getRating()));
        serviceAdapter.updateList(results);
        serviceList.clear();
        serviceList.addAll(results);
        if (results.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            llNotFound.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            llNotFound.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_chats) {
                startActivity(new Intent(this, ChatListActivity.class));
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddPostActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupItemClickListener() {
        serviceAdapter.setOnItemClickListener(position -> {
            Service s = serviceList.get(position);
            Intent i = new Intent(this, ServiceDetailActivity.class);
            i.putExtra("serviceId", s.getServiceId());
            i.putExtra("name", s.getName());
            i.putExtra("profession", s.getProfession());
            i.putExtra("description", s.getDescription());
            i.putExtra("price", s.getPrice());
            i.putExtra("rating", s.getRating());
            i.putExtra("ratingCount", s.getRatingCount());
            i.putExtra("imageResId", s.getImageResId());
            i.putExtra("imageUrl", s.getImageUrl());
            i.putExtra("userId", s.getUserId());
            startActivity(i);
        });
    }
}