package com.example.goprox;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends BaseActivity implements OnMapReadyCallback {

    private RecyclerView recyclerView;
    private ServiceAdapter serviceAdapter;
    private BottomNavigationView bottomNavigationView;
    private TextView tvWelcome;
    private EditText etSearch;
    private ImageView ivAIToggle;
    private LinearLayout llNotFound;
    private FrameLayout mapContainer;
    private LinearLayout listContainer;

    private List<Service> serviceList;
    private List<Service> originalServiceList;

    private FirebaseService firebaseService;
    private ArrayList<String> aiFilterList;

    // Карта
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Map<String, Service> markerServiceMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        serviceList = new ArrayList<>();
        originalServiceList = new ArrayList<>();

        initViews();
        setWelcomeMessage();
        setupRecyclerView();
        setupMap();

        firebaseService = new FirebaseService();
        loadServicesFromFirebase();

        setupAIToggle();
        setupSearch();
        setupBottomNavigation();
        setupItemClickListener();

        aiFilterList = getIntent().getStringArrayListExtra("profession_filter_list");

        // Кнопка переключения на карту (можно добавить в макет FloatingActionButton)
        // Пока просто показываем карту при загрузке, если есть координаты
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tvWelcome = findViewById(R.id.tvWelcome);
        etSearch = findViewById(R.id.etSearch);
        ivAIToggle = findViewById(R.id.ivAIToggle);
        llNotFound = findViewById(R.id.llNotFound);
        mapContainer = findViewById(R.id.mapContainer);
        listContainer = findViewById(R.id.listContainer); // Не забудь добавить id в макет
    }

    // ⬇️ ДОБАВЛЕНО: Инициализация карты
    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Показываем текущее местоположение
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                }
            });
        }
        // Добавляем маркеры специалистов
        addMarkersToMap();
    }

    private void addMarkersToMap() {
        if (mMap == null || serviceList.isEmpty()) return;
        mMap.clear();
        markerServiceMap.clear();

        for (Service service : serviceList) {
            // Предполагаем, что у сервиса есть поля latitude и longitude
            // Если их нет, нужно добавить в модель Service
            double lat = service.getLatitude();
            double lng = service.getLongitude();

            LatLng position = new LatLng(lat, lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(service.getName())
                    .snippet(service.getProfession())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            markerServiceMap.put(marker.getId(), service);
        }

        mMap.setOnMarkerClickListener(marker -> {
            Service service = markerServiceMap.get(marker.getId());
            if (service != null) {
                Intent i = new Intent(HomeActivity.this, ServiceDetailActivity.class);
                i.putExtra("serviceId", service.getId());
                i.putExtra("name", service.getName());
                i.putExtra("profession", service.getProfession());
                i.putExtra("description", service.getDescription());
                i.putExtra("price", service.getPrice());
                i.putExtra("rating", service.getRating());
                i.putExtra("ratingCount", service.getRatingCount());
                i.putExtra("imageUrl", service.getImageUrl());
                i.putExtra("userId", service.getUserId());
                startActivity(i);
            }
            return true;
        });
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
            if (aiFilterList != null && !aiFilterList.isEmpty()) {
                applyAiFilter();
            } else {
                serviceList.clear();
                serviceList.addAll(originalServiceList);
                Collections.sort(serviceList, (a, b) -> Float.compare(b.getRating(), a.getRating()));
                serviceAdapter.updateList(serviceList);
            }
            updateEmptyView();
            addMarkersToMap(); // Обновляем маркеры после загрузки данных
        });
    }

    private void applyAiFilter() {
        List<Service> filtered = new ArrayList<>();
        for (Service s : originalServiceList) {
            String profLower = s.getProfession().toLowerCase();
            String descLower = s.getDescription() != null ? s.getDescription().toLowerCase() : "";
            StringBuilder allText = new StringBuilder(profLower + " " + descLower);
            if (s.getTags() != null) for (String t : s.getTags()) allText.append(" ").append(t.toLowerCase());

            for (String ft : aiFilterList) {
                if (profLower.contains(ft.trim().toLowerCase()) || allText.toString().contains(ft.trim().toLowerCase())) {
                    filtered.add(s);
                    break;
                }
            }
        }
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterServices(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterServices(String query) {
        List<Service> base = serviceList.isEmpty() ? originalServiceList : serviceList;
        List<Service> filtered = new ArrayList<>();
        if (query.isEmpty()) filtered.addAll(base);
        else {
            String q = query.toLowerCase();
            for (Service s : base) if (s.getName().toLowerCase().contains(q) || s.getProfession().toLowerCase().contains(q)) filtered.add(s);
        }
        Collections.sort(filtered, (a, b) -> Float.compare(b.getRating(), a.getRating()));
        serviceAdapter.updateList(filtered);
        serviceList.clear();
        serviceList.addAll(filtered);
        updateEmptyView();
        addMarkersToMap(); // Обновляем маркеры после фильтрации
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
            Intent i = new Intent(this, ServiceDetailActivity.class);
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