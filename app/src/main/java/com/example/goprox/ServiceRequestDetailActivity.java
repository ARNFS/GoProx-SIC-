package com.example.goprox;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceRequestDetailActivity extends BaseActivity {

    private Toolbar toolbar;

    private TextView tvServiceTitle;
    private TextView tvRequestStatus;

    private TextView tvCustomer;
    private TextView tvProvider;

    private TextView tvRequestedPrice;
    private TextView tvCustomerMessage;

    private TextView tvTimelineRequested;
    private TextView tvTimelineAccepted;
    private TextView tvTimelineStarted;
    private TextView tvTimelineCompleted;
    private TextView tvTimelineRejected;
    private TextView tvTimelineCancelled;

    private Button btnPrimary;
    private Button btnSecondary;
    private Button btnCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ServiceRequestRepository repository;

    private String requestId;
    private ServiceRequest request;

    private boolean actionRunning = false;

    private String serviceTitle = "Service";
    private String customerName = "Customer";
    private String providerName = "Provider";

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_service_request_detail
        );

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        repository = new ServiceRequestRepository();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please sign in",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        requestId = getIntent().getStringExtra("requestId");

        if (requestId == null
                || requestId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid request",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadRequest();
    }

    private void initViews() {

        toolbar = findViewById(R.id.toolbar);

        tvServiceTitle = findViewById(R.id.tvServiceTitle);
        tvRequestStatus = findViewById(R.id.tvRequestStatus);

        tvCustomer = findViewById(R.id.tvCustomer);
        tvProvider = findViewById(R.id.tvProvider);

        tvRequestedPrice = findViewById(
                R.id.tvRequestedPrice
        );

        tvCustomerMessage = findViewById(
                R.id.tvCustomerMessage
        );

        tvTimelineRequested = findViewById(
                R.id.tvTimelineRequested
        );

        tvTimelineAccepted = findViewById(
                R.id.tvTimelineAccepted
        );

        tvTimelineStarted = findViewById(
                R.id.tvTimelineStarted
        );

        tvTimelineCompleted = findViewById(
                R.id.tvTimelineCompleted
        );

        tvTimelineRejected = findViewById(
                R.id.tvTimelineRejected
        );

        tvTimelineCancelled = findViewById(
                R.id.tvTimelineCancelled
        );

        btnPrimary = findViewById(R.id.btnPrimary);
        btnSecondary = findViewById(R.id.btnSecondary);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupToolbar() {

        if (toolbar == null) {
            return;
        }

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    "Request Details"
            );

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadRequest() {

        setActionButtonsEnabled(false);

        repository.getRequest(requestId)
                .addOnSuccessListener(snapshot -> {

                    if (snapshot == null
                            || !snapshot.exists()) {

                        Toast.makeText(
                                this,
                                "Request not found",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                        return;
                    }

                    request = snapshot.toObject(
                            ServiceRequest.class
                    );

                    if (request == null) {

                        Toast.makeText(
                                this,
                                "Invalid request data",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                        return;
                    }

                    loadRelatedData();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            getLoadError(e),
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                });
    }

    private void loadRelatedData() {

        loadServiceTitle();

        loadUserName(
                request.getCustomerId(),
                true
        );

        loadUserName(
                request.getProviderId(),
                false
        );

        displayRequest();
    }

    private void loadServiceTitle() {

        String serviceId = request.getServiceId();

        if (serviceId == null
                || serviceId.trim().isEmpty()) {
            return;
        }

        db.collection("services")
                .document(serviceId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot != null
                            && snapshot.exists()) {

                        String title = snapshot.getString(
                                "name"
                        );

                        if (title == null
                                || title.trim().isEmpty()) {

                            title = snapshot.getString(
                                    "title"
                            );
                        }

                        if (title != null
                                && !title.trim().isEmpty()) {

                            serviceTitle = title.trim();

                            if (tvServiceTitle != null) {
                                tvServiceTitle.setText(
                                        serviceTitle
                                );
                            }
                        }
                    }
                });
    }

    private void loadUserName(
            String uid,
            boolean customer
    ) {

        if (uid == null
                || uid.trim().isEmpty()) {
            return;
        }

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot == null
                            || !snapshot.exists()) {
                        return;
                    }

                    String name = getUserDisplayName(
                            snapshot
                    );

                    if (customer) {
                        customerName = name;

                        if (tvCustomer != null) {
                            tvCustomer.setText(
                                    customerName
                            );
                        }
                    } else {
                        providerName = name;

                        if (tvProvider != null) {
                            tvProvider.setText(
                                    providerName
                            );
                        }
                    }
                });
    }

    private String getUserDisplayName(
            DocumentSnapshot snapshot
    ) {

        String name = snapshot.getString("name");

        if (name == null
                || name.trim().isEmpty()) {

            name = snapshot.getString("displayName");
        }

        if (name == null
                || name.trim().isEmpty()) {

            name = snapshot.getString("fullName");
        }

        if (name == null
                || name.trim().isEmpty()) {

            return "Unknown user";
        }

        return name.trim();
    }

    private void displayRequest() {

        if (request == null) {
            return;
        }

        tvServiceTitle.setText(serviceTitle);

        tvCustomer.setText(customerName);
        tvProvider.setText(providerName);

        String price = safeValue(
                request.getRequestedPrice()
        );

        String priceType = request.getPriceType();

        if (priceType != null
                && !priceType.trim().isEmpty()) {

            price += " / " + priceType.trim();
        }

        tvRequestedPrice.setText(price);

        String message = request.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            tvCustomerMessage.setText(
                    "No message provided."
            );

        } else {

            tvCustomerMessage.setText(
                    message.trim()
            );
        }

        updateStatusUI();
        updateTimeline();
        setupActions();
    }

    private void updateStatusUI() {

        String status = request.getStatus();

        tvRequestStatus.setText(
                getStatusLabel(status)
        );

        if (ServiceRequest.STATUS_REQUESTED.equals(status)) {

            tvRequestStatus.setTextColor(
                    getColorSafe(R.color.yellow)
            );

        } else if (
                ServiceRequest.STATUS_COMPLETED.equals(status)
        ) {

            tvRequestStatus.setTextColor(
                    getColorSafe(R.color.green)
            );

        } else if (
                ServiceRequest.STATUS_REJECTED.equals(status)
        ) {

            tvRequestStatus.setTextColor(
                    getColorSafe(R.color.red)
            );

        } else if (
                ServiceRequest.STATUS_CANCELLED.equals(status)
        ) {

            tvRequestStatus.setTextColor(
                    getColorSafe(R.color.gray)
            );

        } else {

            tvRequestStatus.setTextColor(
                    getColorSafe(R.color.primary)
            );
        }
    }

    private void updateTimeline() {

        setTimelineText(
                tvTimelineRequested,
                "Request sent",
                request.getCreatedAt(),
                true
        );

        setTimelineText(
                tvTimelineAccepted,
                "Request accepted",
                request.getAcceptedAt(),
                false
        );

        setTimelineText(
                tvTimelineStarted,
                "Service started",
                request.getStartedAt(),
                false
        );

        setTimelineText(
                tvTimelineCompleted,
                "Service completed",
                request.getCompletedAt(),
                false
        );

        setTimelineText(
                tvTimelineRejected,
                "Request rejected",
                request.getRejectedAt(),
                false
        );

        setTimelineText(
                tvTimelineCancelled,
                "Request cancelled",
                request.getCancelledAt(),
                false
        );
    }

    private void setTimelineText(
            TextView view,
            String label,
            Timestamp timestamp,
            boolean alwaysVisible
    ) {

        if (view == null) {
            return;
        }

        if (timestamp == null) {

            if (alwaysVisible) {
                view.setText(
                        "• " + label + "\n" +
                                "   Time unavailable"
                );

                view.setVisibility(View.VISIBLE);

            } else {

                view.setVisibility(View.GONE);
            }

            return;
        }

        view.setText(
                "• " + label + "\n" +
                        "   " + formatTimestamp(timestamp)
        );

        view.setVisibility(View.VISIBLE);
    }

    private String formatTimestamp(
            Timestamp timestamp
    ) {

        if (timestamp == null) {
            return "Unknown time";
        }

        Date date = timestamp.toDate();

        DateFormat formatter =
                DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT,
                        Locale.getDefault()
                );

        return formatter.format(date);
    }

    private void setupActions() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null
                || request == null) {
            return;
        }

        String uid = user.getUid();

        boolean isProvider =
                uid.equals(request.getProviderId());

        boolean isCustomer =
                uid.equals(request.getCustomerId());

        hideAllActionButtons();

        if (isProvider) {

            if (request.canProviderAccept()) {

                showPrimaryButton(
                        "Accept",
                        v -> acceptRequest()
                );

                showSecondaryButton(
                        "Reject",
                        v -> showRejectDialog()
                );

            } else if (request.canProviderStart()) {

                showPrimaryButton(
                        "Start Service",
                        v -> startRequest()
                );

            } else if (request.canProviderComplete()) {

                showPrimaryButton(
                        "Complete Service",
                        v -> completeRequest()
                );
            }

        } else if (isCustomer) {

            if (request.canCustomerCancel()) {

                showCancelButton(
                        "Cancel Request",
                        v -> showCancelDialog()
                );
            }
        }

        setActionButtonsEnabled(true);
    }

    private void acceptRequest() {

        if (actionRunning
                || request == null) {
            return;
        }

        runAction(
                "Accepting...",
                () -> repository.acceptRequest(
                        requestId
                )
        );
    }

    private void startRequest() {

        if (actionRunning
                || request == null) {
            return;
        }

        runAction(
                "Starting...",
                () -> repository.startRequest(
                        requestId
                )
        );
    }

    private void completeRequest() {

        if (actionRunning
                || request == null) {
            return;
        }

        runAction(
                "Completing...",
                () -> repository.completeRequest(
                        requestId
                )
        );
    }

    private void showRejectDialog() {

        if (actionRunning
                || request == null) {
            return;
        }

        final EditText input =
                new EditText(this);

        input.setHint(
                "Optional reason"
        );

        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        input.setMinLines(3);

        int padding = (int) (
                16 * getResources()
                        .getDisplayMetrics()
                        .density
        );

        input.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        new AlertDialog.Builder(this)
                .setTitle("Reject Request")
                .setMessage(
                        "You can optionally explain why " +
                                "you are rejecting this request."
                )
                .setView(input)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Reject",
                        (dialog, which) -> {

                            String reason =
                                    input.getText()
                                            .toString()
                                            .trim();

                            rejectRequest(reason);
                        }
                )
                .show();
    }

    private void rejectRequest(
            String reason
    ) {

        if (actionRunning
                || request == null) {
            return;
        }

        runAction(
                "Rejecting...",
                () -> repository.rejectRequest(
                        requestId,
                        reason
                )
        );
    }

    private void showCancelDialog() {

        if (actionRunning
                || request == null) {
            return;
        }

        final EditText input =
                new EditText(this);

        input.setHint(
                "Optional reason"
        );

        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        input.setMinLines(3);

        int padding = (int) (
                16 * getResources()
                        .getDisplayMetrics()
                        .density
        );

        input.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        new AlertDialog.Builder(this)
                .setTitle("Cancel Request")
                .setMessage(
                        "Are you sure you want to cancel " +
                                "this request?"
                )
                .setView(input)
                .setNegativeButton(
                        "Keep",
                        null
                )
                .setPositiveButton(
                        "Cancel Request",
                        (dialog, which) -> {

                            String reason =
                                    input.getText()
                                            .toString()
                                            .trim();

                            cancelRequest(reason);
                        }
                )
                .show();
    }

    private void cancelRequest(
            String reason
    ) {

        if (actionRunning
                || request == null) {
            return;
        }

        runAction(
                "Cancelling...",
                () -> repository.cancelRequest(
                        requestId,
                        reason
                )
        );
    }

    private interface RequestAction {
        Task<Void> run();
    }

    private void runAction(
            String loadingText,
            RequestAction action
    ) {

        actionRunning = true;

        setButtonsLoading(loadingText);

        action.run()
                .addOnSuccessListener(
                        unused -> {

                            actionRunning = false;

                            Toast.makeText(
                                    this,
                                    "Request updated",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadRequest();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            actionRunning = false;

                            Toast.makeText(
                                    this,
                                    getActionError(e),
                                    Toast.LENGTH_LONG
                            ).show();

                            setupActions();
                        }
                );
    }

    private String getActionError(
            Exception e
    ) {

        if (e == null) {
            return "Action failed";
        }

        String message = e.getMessage();

        if (message == null
                || message.trim().isEmpty()) {
            return "Action failed";
        }

        if (message.contains(
                "PERMISSION_DENIED"
        )) {
            return "You are not allowed to perform this action";
        }

        if (message.contains(
                "Invalid request status transition"
        )) {
            return "This request has already changed";
        }

        return "Action failed";
    }

    private String getLoadError(
            Exception e
    ) {

        if (e == null) {
            return "Failed to load request";
        }

        String message = e.getMessage();

        if (message != null
                && message.contains(
                "PERMISSION_DENIED"
        )) {
            return "You are not allowed to view this request";
        }

        return "Failed to load request";
    }

    private void hideAllActionButtons() {

        if (btnPrimary != null) {
            btnPrimary.setVisibility(
                    View.GONE
            );
        }

        if (btnSecondary != null) {
            btnSecondary.setVisibility(
                    View.GONE
            );
        }

        if (btnCancel != null) {
            btnCancel.setVisibility(
                    View.GONE
            );
        }
    }

    private void showPrimaryButton(
            String text,
            View.OnClickListener listener
    ) {

        if (btnPrimary == null) {
            return;
        }

        btnPrimary.setText(text);
        btnPrimary.setVisibility(
                View.VISIBLE
        );
        btnPrimary.setOnClickListener(listener);
    }

    private void showSecondaryButton(
            String text,
            View.OnClickListener listener
    ) {

        if (btnSecondary == null) {
            return;
        }

        btnSecondary.setText(text);
        btnSecondary.setVisibility(
                View.VISIBLE
        );
        btnSecondary.setOnClickListener(listener);
    }

    private void showCancelButton(
            String text,
            View.OnClickListener listener
    ) {

        if (btnCancel == null) {
            return;
        }

        btnCancel.setText(text);
        btnCancel.setVisibility(
                View.VISIBLE
        );
        btnCancel.setOnClickListener(listener);
    }

    private void setActionButtonsEnabled(
            boolean enabled
    ) {

        if (btnPrimary != null) {
            btnPrimary.setEnabled(enabled);
        }

        if (btnSecondary != null) {
            btnSecondary.setEnabled(enabled);
        }

        if (btnCancel != null) {
            btnCancel.setEnabled(enabled);
        }
    }

    private void setButtonsLoading(
            String text
    ) {

        if (btnPrimary != null
                && btnPrimary.getVisibility()
                == View.VISIBLE) {

            btnPrimary.setText(text);
        }

        if (btnSecondary != null
                && btnSecondary.getVisibility()
                == View.VISIBLE) {

            btnSecondary.setEnabled(false);
        }

        if (btnCancel != null
                && btnCancel.getVisibility()
                == View.VISIBLE) {

            btnCancel.setEnabled(false);
        }

        if (btnPrimary != null) {
            btnPrimary.setEnabled(false);
        }
    }

    private String getStatusLabel(
            String status
    ) {

        if (ServiceRequest.STATUS_REQUESTED.equals(status)) {
            return "●  Waiting for response";
        }

        if (ServiceRequest.STATUS_ACCEPTED.equals(status)) {
            return "●  Accepted";
        }

        if (ServiceRequest.STATUS_IN_PROGRESS.equals(status)) {
            return "●  Service in progress";
        }

        if (ServiceRequest.STATUS_COMPLETED.equals(status)) {
            return "●  Completed";
        }

        if (ServiceRequest.STATUS_REJECTED.equals(status)) {
            return "●  Rejected";
        }

        if (ServiceRequest.STATUS_CANCELLED.equals(status)) {
            return "●  Cancelled";
        }

        return "●  Unknown status";
    }

    private String safeValue(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return "Not specified";
        }

        return value.trim();
    }

    private int getColorSafe(
            int colorRes
    ) {

        return getResources().getColor(
                colorRes,
                getTheme()
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}