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

public class HomeActivity extends BaseActivity {

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

    // Фильтр, переданный из AI
    private ArrayList<String> aiFilterList;

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

        // Получаем фильтр от AI (если есть)
        aiFilterList = getIntent().getStringArrayListExtra("profession_filter_list");
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
        if (tvWelcome != null) tvWelcome.setText("Welcome, " + userName + "!");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceAdapter(serviceList);
        recyclerView.setAdapter(serviceAdapter);
    }

    private void loadServicesFromFirebase() {
        firebaseService.getAllServices(services -> {
            originalServiceList.clear();
            originalServiceList.addAll(services);

            // Применяем AI-фильтр, если он есть
            if (aiFilterList != null && !aiFilterList.isEmpty()) {
                applyAiFilter();
            } else {
                // Иначе показываем все, отсортированные по рейтингу
                serviceList.clear();
                serviceList.addAll(originalServiceList);
                Collections.sort(serviceList, (a, b) -> Float.compare(b.getRating(), a.getRating()));
                serviceAdapter.updateList(serviceList);
            }
            updateEmptyView();
        });
    }

    /**
     * Применяет фильтр по списку профессий/тегов от AI.
     */
    private void applyAiFilter() {
        List<Service> filtered = new ArrayList<>();
        for (Service s : originalServiceList) {
            boolean matches = false;
            String profLower = s.getProfession().toLowerCase();
            String descLower = s.getDescription() != null ? s.getDescription().toLowerCase() : "";

            // Собираем все ключевые слова сервиса: профессия + описание + теги
            StringBuilder allText = new StringBuilder();
            allText.append(profLower).append(" ").append(descLower);
            if (s.getTags() != null) {
                for (String tag : s.getTags()) {
                    allText.append(" ").append(tag.toLowerCase());
                }
            }

            for (String filterTitle : aiFilterList) {
                String ft = filterTitle.toLowerCase().trim();
                // Проверяем, содержит ли профессия фильтр, или фильтр содержится в общем тексте
                if (profLower.contains(ft) || allText.toString().contains(ft)) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                filtered.add(s);
            }
        }
        // Сортируем по рейтингу
        Collections.sort(filtered, (a, b) -> Float.compare(b.getRating(), a.getRating()));
        serviceList.clear();
        serviceList.addAll(filtered);
        serviceAdapter.updateList(serviceList);
    }

    private void updateEmptyView() {
        if (serviceList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            llNotFound.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            llNotFound.setVisibility(View.GONE);
        }
    }

    private void setupAIToggle() {
        ivAIToggle.setOnClickListener(v -> startActivity(new Intent(this, AIDialogActivity.class)));
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
        List<Service> baseList = serviceList.isEmpty() ? originalServiceList : serviceList;
        List<Service> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(baseList);
        } else {
            String q = query.toLowerCase();
            for (Service s : baseList) {
                if (s.getName().toLowerCase().contains(q) || s.getProfession().toLowerCase().contains(q)) {
                    filtered.add(s);
                }
            }
        }
        Collections.sort(filtered, (a, b) -> Float.compare(b.getRating(), a.getRating()));
        serviceAdapter.updateList(filtered);
        serviceList.clear();
        serviceList.addAll(filtered);
        updateEmptyView();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_chats) startActivity(new Intent(this, ChatListActivity.class));
            if (id == R.id.nav_add) startActivity(new Intent(this, AddPostActivity.class));
            if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupItemClickListener() {
        serviceAdapter.setOnItemClickListener(position -> {
            Service s = serviceList.get(position);
            Intent i = new Intent(HomeActivity.this, ServiceDetailActivity.class);
            i.putExtra("serviceId", s.getId());
            i.putExtra("name", s.getName());
            i.putExtra("profession", s.getProfession());
            i.putExtra("description", s.getDescription());
            i.putExtra("price", s.getPrice());
            i.putExtra("rating", s.getRating());
            i.putExtra("ratingCount", s.getRatingCount());
            i.putExtra("imageUrl", s.getImageUrl());
            i.putExtra("userId", s.getUserId());
            startActivity(i);
        });
    }
}