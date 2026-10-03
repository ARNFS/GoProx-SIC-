package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ServiceRequestsActivity extends BaseActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvLoading;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private FirebaseUser currentUser;

    private ServiceRequestAdapter adapter;

    private final List<ServiceRequest> requestList =
            new ArrayList<>();

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
        loadRequests();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerViewRequests);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvLoading = findViewById(R.id.tvLoading);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("My Requests");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new ServiceRequestAdapter(
                requestList,
                showingIncoming,
                request -> {
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
        );

        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        if (tabLayout == null) return;

        tabLayout.addTab(
                tabLayout.newTab().setText("Incoming")
        );

        tabLayout.addTab(
                tabLayout.newTab().setText("Sent")
        );

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(
                            TabLayout.Tab tab
                    ) {
                        showingIncoming =
                                tab.getPosition() == 0;

                        loadRequests();
                    }

                    @Override
                    public void onTabUnselected(
                            TabLayout.Tab tab
                    ) {
                    }

                    @Override
                    public void onTabReselected(
                            TabLayout.Tab tab
                    ) {
                    }
                }
        );
    }

    private void loadRequests() {

        if (currentUser == null) return;

        setLoading(true);

        requestList.clear();

        String uid = currentUser.getUid();

        Query query;

        if (showingIncoming) {
            query = db.collection("serviceRequests")
                    .whereEqualTo("providerId", uid)
                    .orderBy(
                            "createdAt",
                            Query.Direction.DESCENDING
                    );
        } else {
            query = db.collection("serviceRequests")
                    .whereEqualTo("customerId", uid)
                    .orderBy(
                            "createdAt",
                            Query.Direction.DESCENDING
                    );
        }

        query.get()
                .addOnSuccessListener(snapshot -> {

                    requestList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        ServiceRequest request =
                                doc.toObject(
                                        ServiceRequest.class
                                );

                        if (request != null) {
                            requestList.add(request);
                        }
                    }

                    if (adapter != null) {
                        adapter.setIncomingMode(
                                showingIncoming
                        );

                        adapter.notifyDataSetChanged();
                    }

                    setLoading(false);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {

                    setLoading(false);

                    requestList.clear();

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    updateEmptyState();

                    Toast.makeText(
                            this,
                            "Failed to load requests",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void setLoading(boolean loading) {

        if (tvLoading != null) {
            tvLoading.setVisibility(
                    loading
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (recyclerView != null) {
            recyclerView.setVisibility(
                    loading
                            ? View.GONE
                            : View.VISIBLE
            );
        }

        if (tvEmpty != null && loading) {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {

        if (tvEmpty == null) return;

        if (requestList.isEmpty()) {

            tvEmpty.setVisibility(View.VISIBLE);

            tvEmpty.setText(
                    showingIncoming
                            ? "No incoming requests yet"
                            : "You haven't sent any requests yet"
            );

            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }

        } else {

            tvEmpty.setVisibility(View.GONE);

            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUser != null) {
            loadRequests();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}