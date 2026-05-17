package com.example.goprox;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends BaseActivity implements OnMapReadyCallback {

    private RecyclerView recyclerView;
    private ServiceAdapter serviceAdapter;
    private BottomNavigationView bottomNavigationView;
    private TextView tvWelcome;
    private EditText etSearch;
    private ImageView ivAIToggle, ivFilter;
    private LinearLayout llNotFound;

    private final List<Service> serviceList = new ArrayList<>();
    private final List<Service> originalServiceList = new ArrayList<>();

    private FirebaseService firebaseService;
    private ArrayList<String> aiFilterList;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final Map<String, Service> markerServiceMap = new HashMap<>();

    // Filter state
    private Set<String> selectedProfessions = new HashSet<>();
    private int minPrice = -1, maxPrice = -1;
    private float minRating = 0;
    private String filterCity = "", filterCountry = "";
    private boolean isFiltered = false;

    // City/Country lists
    private List<String> cityList = new ArrayList<>();
    private List<String> countryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        aiFilterList = getIntent().getStringArrayListExtra("profession_filter_list");
        loadCityCountryLists();

        initViews();
        setWelcomeMessage();
        setupRecyclerView();
        setupMap();

        firebaseService = new FirebaseService();
        loadServicesFromFirebase();

        setupAIToggle();
        setupFilterButton();
        setupSearch();
        setupBottomNavigation();
        setupItemClickListener();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        aiFilterList = intent.getStringArrayListExtra("profession_filter_list");
        if (firebaseService != null) {
            loadServicesFromFirebase();
        }
    }

    // 🔥 Բեռնում ենք city/country ցուցակները
    private void loadCityCountryLists() {
        cityList = readRawFile(R.raw.cities);
        countryList = readRawFile(R.raw.countries);
    }

    private List<String> readRawFile(int resId) {
        List<String> list = new ArrayList<>();
        try {
            InputStream is = getResources().openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) list.add(line.trim());
            }
            reader.close();
        } catch (Exception ignored) {}
        return list;
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tvWelcome = findViewById(R.id.tvWelcome);
        etSearch = findViewById(R.id.etSearch);
        ivAIToggle = findViewById(R.id.ivAIToggle);
        ivFilter = findViewById(R.id.ivFilter);
        llNotFound = findViewById(R.id.llNotFound);

        if (recyclerView == null || bottomNavigationView == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        } catch (Exception e) {
            fusedLocationClient = null;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) return;
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } catch (Exception ignored) {}
        if (fusedLocationClient != null) {
            try {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null && mMap != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                        }
                    });
                }
            } catch (Exception ignored) {}
        }
        addMarkersToMap();
    }

    private void addMarkersToMap() {
        if (mMap == null) return;
        mMap.clear();
        markerServiceMap.clear();
        synchronized (serviceList) {
            for (Service service : serviceList) {
                if (service == null) continue;
                try {
                    double lat = service.getLatitude();
                    double lng = service.getLongitude();
                    if (lat == 0.0 && lng == 0.0) continue;
                    LatLng position = new LatLng(lat, lng);
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(service.getName())
                            .snippet(service.getProfession())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    if (marker != null) markerServiceMap.put(marker.getId(), service);
                } catch (Exception ignored) {}
            }
        }
        mMap.setOnMarkerClickListener(marker -> {
            if (marker == null) return false;
            Service service = markerServiceMap.get(marker.getId());
            if (service != null) { openServiceDetail(service); return true; }
            return false;
        });
    }

    private void openServiceDetail(Service service) {
        if (service == null) return;
        Intent i = new Intent(this, ServiceDetailActivity.class);
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

    private void setWelcomeMessage() {
        if (tvWelcome == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "User";
        tvWelcome.setText("Welcome, " + userName + "!");
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceAdapter(serviceList);
        recyclerView.setAdapter(serviceAdapter);
    }

    private void loadServicesFromFirebase() {
        if (firebaseService == null) return;
        firebaseService.getAllServices(services -> {
            runOnUiThread(() -> {
                synchronized (originalServiceList) {
                    originalServiceList.clear();
                    if (services != null) originalServiceList.addAll(services);
                }
                if (aiFilterList != null && !aiFilterList.isEmpty()) {
                    applyAiFilter();
                } else if (isFiltered) {
                    applyManualFilter();
                } else {
                    synchronized (serviceList) {
                        serviceList.clear();
                        serviceList.addAll(originalServiceList);
                        sortServices(serviceList);
                    }
                    if (serviceAdapter != null) serviceAdapter.updateList(new ArrayList<>(serviceList));
                }
                updateEmptyView();
                addMarkersToMap();
            });
        });
    }

    private void applyAiFilter() {
        if (aiFilterList == null) return;
        List<Service> filtered = new ArrayList<>();
        synchronized (originalServiceList) {
            for (Service s : originalServiceList) {
                if (s == null) continue;
                String profLower = s.getProfession() != null ? s.getProfession().toLowerCase() : "";
                String descLower = s.getDescription() != null ? s.getDescription().toLowerCase() : "";
                StringBuilder allText = new StringBuilder(profLower + " " + descLower);
                if (s.getTags() != null) for (String t : s.getTags()) if (t != null) allText.append(" ").append(t.toLowerCase());
                for (String ft : aiFilterList) {
                    if (ft == null) continue;
                    if (profLower.contains(ft.trim().toLowerCase()) || allText.toString().contains(ft.trim().toLowerCase())) {
                        filtered.add(s); break;
                    }
                }
            }
        }
        sortServices(filtered);
        synchronized (serviceList) { serviceList.clear(); serviceList.addAll(filtered); }
        if (serviceAdapter != null) serviceAdapter.updateList(new ArrayList<>(serviceList));
    }

    private void applyManualFilter() {
        List<Service> filtered = new ArrayList<>();
        synchronized (originalServiceList) {
            for (Service s : originalServiceList) {
                if (s == null) continue;

                if (!selectedProfessions.isEmpty()) {
                    String prof = s.getProfession() != null ? s.getProfession() : "";
                    if (!selectedProfessions.contains(prof)) continue;
                }

                if (minPrice >= 0 || maxPrice >= 0) {
                    int price = 0;
                    try {
                        String p = s.getPrice() != null ? s.getPrice().replaceAll("[^0-9]", "") : "0";
                        price = p.isEmpty() ? 0 : Integer.parseInt(p);
                    } catch (Exception ignored) {}
                    if (minPrice >= 0 && price < minPrice) continue;
                    if (maxPrice >= 0 && price > maxPrice) continue;
                }

                if (minRating > 0 && s.getRating() < minRating) continue;

                if (!filterCity.isEmpty()) {
                    String city = s.getCity() != null ? s.getCity().toLowerCase() : "";
                    if (!city.contains(filterCity.toLowerCase())) continue;
                }

                if (!filterCountry.isEmpty()) {
                    String country = s.getCountry() != null ? s.getCountry().toLowerCase() : "";
                    if (!country.contains(filterCountry.toLowerCase())) continue;
                }

                filtered.add(s);
            }
        }
        sortServices(filtered);
        synchronized (serviceList) { serviceList.clear(); serviceList.addAll(filtered); }
        if (serviceAdapter != null) serviceAdapter.updateList(new ArrayList<>(serviceList));
    }

    private void sortServices(List<Service> list) {
        if (list == null) return;
        Collections.sort(list, (a, b) -> {
            if (a == null || b == null) return 0;
            return Float.compare(b.getRating(), a.getRating());
        });
    }

    private void updateEmptyView() {
        if (recyclerView == null || llNotFound == null) return;
        if (serviceList.isEmpty()) { recyclerView.setVisibility(View.GONE); llNotFound.setVisibility(View.VISIBLE); }
        else { recyclerView.setVisibility(View.VISIBLE); llNotFound.setVisibility(View.GONE); }
    }

    private void setupAIToggle() {
        if (ivAIToggle != null) ivAIToggle.setOnClickListener(v -> startActivity(new Intent(this, AIDialogActivity.class)));
    }

    private void setupFilterButton() {
        if (ivFilter != null) ivFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter, null);
        builder.setView(view);

        LinearLayout llProfessions = view.findViewById(R.id.llProfessions);
        EditText etMinPrice = view.findViewById(R.id.etMinPrice);
        EditText etMaxPrice = view.findViewById(R.id.etMaxPrice);
        RatingBar ratingBarFilter = view.findViewById(R.id.ratingBarFilter);
        AutoCompleteTextView etCity = view.findViewById(R.id.etCity);
        AutoCompleteTextView etCountry = view.findViewById(R.id.etCountry);
        Button btnApply = view.findViewById(R.id.btnApplyFilter);
        Button btnReset = view.findViewById(R.id.btnResetFilter);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cityList);
        etCity.setAdapter(cityAdapter);

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countryList);
        etCountry.setAdapter(countryAdapter);

        if (minPrice >= 0) etMinPrice.setText(String.valueOf(minPrice));
        if (maxPrice >= 0) etMaxPrice.setText(String.valueOf(maxPrice));
        ratingBarFilter.setRating(minRating);
        etCity.setText(filterCity);
        etCountry.setText(filterCountry);

        llProfessions.removeAllViews();
        Set<String> uniqueProfessions = new HashSet<>();
        for (Service s : originalServiceList) {
            if (s != null && s.getProfession() != null) uniqueProfessions.add(s.getProfession());
        }
        for (String prof : uniqueProfessions) {
            CheckBox cb = new CheckBox(this);
            cb.setText(prof);
            cb.setChecked(selectedProfessions.contains(prof));
            llProfessions.addView(cb);
        }

        AlertDialog dialog = builder.create();

        btnApply.setOnClickListener(v -> {
            selectedProfessions.clear();
            for (int i = 0; i < llProfessions.getChildCount(); i++) {
                View child = llProfessions.getChildAt(i);
                if (child instanceof CheckBox) {
                    CheckBox cb = (CheckBox) child;
                    if (cb.isChecked()) selectedProfessions.add(cb.getText().toString());
                }
            }
            try { minPrice = etMinPrice.getText().toString().isEmpty() ? -1 : Integer.parseInt(etMinPrice.getText().toString()); } catch (Exception e) { minPrice = -1; }
            try { maxPrice = etMaxPrice.getText().toString().isEmpty() ? -1 : Integer.parseInt(etMaxPrice.getText().toString()); } catch (Exception e) { maxPrice = -1; }
            minRating = ratingBarFilter.getRating();
            filterCity = etCity.getText().toString().trim();
            filterCountry = etCountry.getText().toString().trim();

            isFiltered = !selectedProfessions.isEmpty() || minPrice >= 0 || maxPrice >= 0 || minRating > 0 || !filterCity.isEmpty() || !filterCountry.isEmpty();
            applyManualFilter();
            updateEmptyView();
            addMarkersToMap();
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            selectedProfessions.clear();
            minPrice = -1; maxPrice = -1; minRating = 0;
            filterCity = ""; filterCountry = ""; isFiltered = false;
            etMinPrice.setText(""); etMaxPrice.setText("");
            ratingBarFilter.setRating(0);
            etCity.setText(""); etCountry.setText("");
            for (int i = 0; i < llProfessions.getChildCount(); i++) {
                View child = llProfessions.getChildAt(i);
                if (child instanceof CheckBox) ((CheckBox) child).setChecked(false);
            }
            synchronized (serviceList) { serviceList.clear(); serviceList.addAll(originalServiceList); sortServices(serviceList); }
            if (serviceAdapter != null) serviceAdapter.updateList(new ArrayList<>(serviceList));
            updateEmptyView();
            addMarkersToMap();
        });

        // 🔥 Որ keyboard-ը dialog-ը վերև հրի
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterServices(s != null ? s.toString() : ""); }
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterServices(String query) {
        if (serviceAdapter == null) return;
        List<Service> base = serviceList.isEmpty() ? originalServiceList : serviceList;
        List<Service> filtered = new ArrayList<>();
        if (query == null || query.isEmpty()) { filtered.addAll(base); }
        else {
            String q = query.toLowerCase();
            for (Service s : base) {
                if (s == null) continue;
                if ((s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                        (s.getProfession() != null && s.getProfession().toLowerCase().contains(q))) {
                    filtered.add(s);
                }
            }
        }
        sortServices(filtered);
        serviceAdapter.updateList(filtered);
        synchronized (serviceList) { serviceList.clear(); serviceList.addAll(filtered); }
        updateEmptyView();
        addMarkersToMap();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_chats) { startActivity(new Intent(this, ChatListActivity.class)); return true; }
            if (id == R.id.nav_add) { startActivity(new Intent(this, AddPostActivity.class)); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); return true; }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupItemClickListener() {
        if (serviceAdapter == null) return;
        serviceAdapter.setOnItemClickListener(position -> {
            synchronized (serviceList) {
                if (position < 0 || position >= serviceList.size()) return;
                Service s = serviceList.get(position);
                if (s != null) openServiceDetail(s);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMap != null) { mMap.clear(); mMap = null; }
        markerServiceMap.clear();
    }
}