package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRequestsActivity extends BaseActivity {

    private Toolbar toolbar;

    private MaterialButton btnIncoming;
    private MaterialButton btnSent;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvSubtitle;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ServiceRequestAdapter adapter;

    private final List<ServiceRequest> requestList = new ArrayList<>();

    private final Map<String, String> serviceNames = new HashMap<>();
    private final Map<String, String> serviceImages = new HashMap<>();
    private final Map<String, String> userNames = new HashMap<>();
    private final Map<String, String> userPhotos = new HashMap<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private boolean showingIncoming = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_requests);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Please sign in",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupTabs();
        setupRefresh();

        loadRequests();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        btnIncoming = findViewById(R.id.btnIncoming);
        btnSent = findViewById(R.id.btnSent);

        recyclerView = findViewById(R.id.recyclerViewRequests);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        swipeRefreshLayout = findViewById(
                R.id.swipeRefreshLayout
        );
    }

    private void setupToolbar() {
        if (toolbar == null) return;

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Requests");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerView.setHasFixedSize(false);

        adapter = new ServiceRequestAdapter(
                requestList,
                serviceNames,
                serviceImages,
                userNames,
                userPhotos,
                request -> openRequest(request)
        );

        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        if (btnIncoming != null) {
            btnIncoming.setOnClickListener(v -> {
                if (!showingIncoming) {
                    showingIncoming = true;
                    updateTabAppearance();
                    loadRequests();
                }
            });
        }

        if (btnSent != null) {
            btnSent.setOnClickListener(v -> {
                if (showingIncoming) {
                    showingIncoming = false;
                    updateTabAppearance();
                    loadRequests();
                }
            });
        }

        updateTabAppearance();
    }

    private void updateTabAppearance() {
        if (btnIncoming == null || btnSent == null) {
            return;
        }

        if (showingIncoming) {
            btnIncoming.setBackgroundTintList(
                    getColorStateList(R.color.primary)
            );

            btnIncoming.setTextColor(
                    getColor(android.R.color.white)
            );

            btnSent.setBackgroundTintList(
                    getColorStateList(R.color.light_gray)
            );

            btnSent.setTextColor(
                    getColor(R.color.dark_gray)
            );

            if (tvSubtitle != null) {
                tvSubtitle.setText(
                        "Requests from customers"
                );
            }

        } else {
            btnSent.setBackgroundTintList(
                    getColorStateList(R.color.primary)
            );

            btnSent.setTextColor(
                    getColor(android.R.color.white)
            );

            btnIncoming.setBackgroundTintList(
                    getColorStateList(R.color.light_gray)
            );

            btnIncoming.setTextColor(
                    getColor(R.color.dark_gray)
            );

            if (tvSubtitle != null) {
                tvSubtitle.setText(
                        "Requests you have sent"
                );
            }
        }
    }

    private void setupRefresh() {
        if (swipeRefreshLayout == null) return;

        swipeRefreshLayout.setOnRefreshListener(
                this::loadRequests
        );
    }

    private void loadRequests() {
        if (currentUser == null) return;

        setLoading(true);

        requestList.clear();
        serviceNames.clear();
        serviceImages.clear();
        userNames.clear();
        userPhotos.clear();

        String uid = currentUser.getUid();

        Query query;

        if (showingIncoming) {
            query = db.collection("serviceRequests")
                    .whereEqualTo("providerId", uid)
                    .orderBy(
                            "updatedAt",
                            Query.Direction.DESCENDING
                    );
        } else {
            query = db.collection("serviceRequests")
                    .whereEqualTo("customerId", uid)
                    .orderBy(
                            "updatedAt",
                            Query.Direction.DESCENDING
                    );
        }

        query.get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot document : snapshot) {
                        ServiceRequest request =
                                document.toObject(
                                        ServiceRequest.class
                                );

                        if (request == null) {
                            continue;
                        }

                        if (request.getRequestId() == null
                                || request.getRequestId().isEmpty()) {

                            request.setRequestId(
                                    document.getId()
                            );
                        }

                        requestList.add(request);
                    }

                    if (requestList.isEmpty()) {
                        showEmptyState(true);
                        setLoading(false);
                        return;
                    }

                    loadRelatedData();

                })
                .addOnFailureListener(e -> {

                    setLoading(false);
                    showEmptyState(true);

                    Toast.makeText(
                            this,
                            getReadableError(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadRelatedData() {
        final int totalRequests = requestList.size();

        final int[] completedLoads = {0};

        for (ServiceRequest request : requestList) {

            loadServiceData(
                    request,
                    () -> {
                        completedLoads[0]++;

                        if (completedLoads[0] >= totalRequests) {
                            finishLoading();
                        }
                    }
            );
        }
    }

    private void loadServiceData(
            ServiceRequest request,
            Runnable onComplete
    ) {
        String serviceId = request.getServiceId();

        if (serviceId == null || serviceId.isEmpty()) {
            loadUserData(
                    getOtherUserId(request),
                    onComplete
            );
            return;
        }

        if (serviceNames.containsKey(serviceId)) {
            loadUserData(
                    getOtherUserId(request),
                    onComplete
            );
            return;
        }

        db.collection("services")
                .document(serviceId)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name =
                                document.getString("name");

                        String imageUrl =
                                document.getString("imageUrl");

                        serviceNames.put(
                                serviceId,
                                name != null
                                        ? name
                                        : "Service"
                        );

                        serviceImages.put(
                                serviceId,
                                imageUrl != null
                                        ? imageUrl
                                        : ""
                        );
                    }

                    loadUserData(
                            getOtherUserId(request),
                            onComplete
                    );
                })
                .addOnFailureListener(e ->
                        loadUserData(
                                getOtherUserId(request),
                                onComplete
                        )
                );
    }

    private void loadUserData(
            String userId,
            Runnable onComplete
    ) {
        if (userId == null || userId.isEmpty()) {
            onComplete.run();
            return;
        }

        if (userNames.containsKey(userId)) {
            onComplete.run();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name =
                                document.getString("name");

                        String photo =
                                document.getString("photoUrl");

                        userNames.put(
                                userId,
                                name != null
                                        ? name
                                        : "User"
                        );

                        userPhotos.put(
                                userId,
                                photo != null
                                        ? photo
                                        : ""
                        );
                    }

                    onComplete.run();
                })
                .addOnFailureListener(e ->
                        onComplete.run()
                );
    }

    private String getOtherUserId(
            ServiceRequest request
    ) {
        if (request == null || currentUser == null) {
            return null;
        }

        if (currentUser.getUid().equals(
                request.getProviderId()
        )) {
            return request.getCustomerId();
        }

        return request.getProviderId();
    }

    private void finishLoading() {
        runOnUiThread(() -> {

            setLoading(false);

            showEmptyState(
                    requestList.isEmpty()
            );

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {

            if (progressBar != null) {
                progressBar.setVisibility(
                        loading
                                ? View.VISIBLE
                                : View.GONE
                );
            }

            if (swipeRefreshLayout != null
                    && !loading) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (recyclerView != null) {
                recyclerView.setVisibility(
                        loading
                                ? View.GONE
                                : View.VISIBLE
                );
            }
        });
    }

    private void showEmptyState(boolean empty) {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(
                    empty
                            ? View.VISIBLE
                            : View.GONE
            );

            if (empty) {
                tvEmpty.setText(
                        showingIncoming
                                ? "No incoming requests yet"
                                : "You haven't sent any requests yet"
                );
            }
        }

        if (recyclerView != null) {
            recyclerView.setVisibility(
                    empty
                            ? View.GONE
                            : View.VISIBLE
            );
        }
    }

    private void openRequest(ServiceRequest request) {
        if (request == null) return;

        Intent intent = new Intent(
                this,
                ServiceRequestDetailActivity.class
        );

        intent.putExtra(
                "requestId",
                request.getRequestId()
        );

        startActivity(intent);
    }

    private String getReadableError(Exception e) {
        if (e == null) {
            return "Failed to load requests";
        }

        String message = e.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return "Failed to load requests";
        }

        if (message.toLowerCase().contains("index")) {
            return "Request list index is not ready yet";
        }

        if (message.toLowerCase().contains(
                "permission"
        )) {
            return "You don't have permission to view these requests";
        }

        return "Failed to load requests";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}