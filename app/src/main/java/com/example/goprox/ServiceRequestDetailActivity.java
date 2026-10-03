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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ServiceRequestDetailActivity extends BaseActivity {

    private Toolbar toolbar;

    private TextView tvServiceTitle;
    private TextView tvCustomer;
    private TextView tvProvider;
    private TextView tvPrice;
    private TextView tvMessage;
    private TextView tvStatus;

    private Button btnPrimary;
    private Button btnSecondary;
    private Button btnCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ServiceRequestRepository repository;

    private String requestId;
    private ServiceRequest request;

    private boolean actionRunning = false;

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

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please sign in",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        requestId =
                getIntent().getStringExtra(
                        "requestId"
                );

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

        toolbar =
                findViewById(R.id.toolbar);

        tvServiceTitle =
                findViewById(
                        R.id.tvServiceTitle
                );

        tvCustomer =
                findViewById(
                        R.id.tvCustomer
                );

        tvProvider =
                findViewById(
                        R.id.tvProvider
                );

        tvPrice =
                findViewById(
                        R.id.tvRequestedPrice
                );

        tvMessage =
                findViewById(
                        R.id.tvCustomerMessage
                );

        tvStatus =
                findViewById(
                        R.id.tvRequestStatus
                );

        btnPrimary =
                findViewById(
                        R.id.btnPrimary
                );

        btnSecondary =
                findViewById(
                        R.id.btnSecondary
                );

        btnCancel =
                findViewById(
                        R.id.btnCancel
                );
    }

    private void setupToolbar() {

        if (toolbar == null) return;

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

                    request =
                            snapshot.toObject(
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

                    displayRequest();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Failed to load request",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });
    }

    private void displayRequest() {

        if (request == null) return;

        tvServiceTitle.setText(
                "Service Request"
        );

        tvCustomer.setText(
                "Customer\n"
                        + safeValue(
                        request.getCustomerId()
                )
        );

        tvProvider.setText(
                "Provider\n"
                        + safeValue(
                        request.getProviderId()
                )
        );

        String price =
                safeValue(
                        request.getRequestedPrice()
                );

        if (request.getPriceType() != null
                && !request.getPriceType()
                .trim()
                .isEmpty()) {

            price += " / "
                    + request.getPriceType();
        }

        tvPrice.setText(price);

        String message =
                request.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            tvMessage.setText(
                    "No message provided."
            );

        } else {
            tvMessage.setText(
                    message.trim()
            );
        }

        tvStatus.setText(
                getStatusLabel(
                        request.getStatus()
                )
        );

        setupActions();
    }

    private void setupActions() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null
                || request == null) {
            return;
        }

        String uid = user.getUid();

        boolean isProvider =
                uid.equals(
                        request.getProviderId()
                );

        boolean isCustomer =
                uid.equals(
                        request.getCustomerId()
                );

        hideAllActionButtons();

        if (isProvider) {

            if (request.canProviderAccept()) {

                showPrimaryButton(
                        "Accept",
                        this::acceptRequest
                );

                showSecondaryButton(
                        "Reject",
                        this::showRejectDialog
                );

            } else if (request.canProviderStart()) {

                showPrimaryButton(
                        "Start Service",
                        this::startRequest
                );

            } else if (request.canProviderComplete()) {

                showPrimaryButton(
                        "Complete Service",
                        this::completeRequest
                );
            }

        } else if (isCustomer) {

            if (request.canCustomerCancel()) {

                showCancelButton(
                        "Cancel Request",
                        this::showCancelDialog
                );
            }
        }

        setActionButtonsEnabled(true);
    }

    private void acceptRequest() {

        if (actionRunning || request == null) return;

        runAction(
                "Accepting...",
                () -> repository.acceptRequest(
                        requestId
                )
        );
    }

    private void startRequest() {

        if (actionRunning || request == null) return;

        runAction(
                "Starting...",
                () -> repository.startRequest(
                        requestId
                )
        );
    }

    private void completeRequest() {

        if (actionRunning || request == null) return;

        runAction(
                "Completing...",
                () -> repository.completeRequest(
                        requestId
                )
        );
    }

    private void showRejectDialog() {

        if (actionRunning || request == null) return;

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

        int padding =
                (int) (
                        20
                                * getResources()
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
                        "You can optionally explain why "
                                + "you are rejecting this request."
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

        if (actionRunning || request == null) return;

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

        new AlertDialog.Builder(this)
                .setTitle("Cancel Request")
                .setMessage(
                        "Are you sure you want to cancel "
                                + "this request?"
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
        com.google.android.gms.tasks.Task<Void> run();
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

        String message =
                e.getMessage();

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

        btnPrimary.setText(text);
        btnPrimary.setVisibility(View.VISIBLE);
        btnPrimary.setOnClickListener(listener);
    }

    private void showSecondaryButton(
            String text,
            View.OnClickListener listener
    ) {

        btnSecondary.setText(text);
        btnSecondary.setVisibility(View.VISIBLE);
        btnSecondary.setOnClickListener(listener);
    }

    private void showCancelButton(
            String text,
            View.OnClickListener listener
    ) {

        btnCancel.setText(text);
        btnCancel.setVisibility(View.VISIBLE);
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

        setActionButtonsEnabled(false);
    }

    private String getStatusLabel(
            String status
    ) {

        if (ServiceRequest.STATUS_REQUESTED.equals(status)) {
            return "🟡 Waiting for response";
        }

        if (ServiceRequest.STATUS_ACCEPTED.equals(status)) {
            return "🔵 Accepted";
        }

        if (ServiceRequest.STATUS_IN_PROGRESS.equals(status)) {
            return "🔵 Service in progress";
        }

        if (ServiceRequest.STATUS_COMPLETED.equals(status)) {
            return "🟢 Completed";
        }

        if (ServiceRequest.STATUS_REJECTED.equals(status)) {
            return "🔴 Rejected";
        }

        if (ServiceRequest.STATUS_CANCELLED.equals(status)) {
            return "⚪ Cancelled";
        }

        return "Unknown status";
    }

    private String safeValue(String value) {

        if (value == null
                || value.trim().isEmpty()) {
            return "Unknown";
        }

        return value.trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
