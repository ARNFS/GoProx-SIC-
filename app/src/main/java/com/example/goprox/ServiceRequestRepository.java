package com.example.goprox;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class ServiceRequestRepository {

    private static final String COLLECTION_REQUESTS = "serviceRequests";
    private static final String COLLECTION_SERVICES = "services";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ServiceRequestRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // =========================================================
    // CREATE REQUEST
    // =========================================================

    /**
     * Creates a new service request.
     *
     * Provider ID is loaded from the service document instead of
     * trusting the client to provide an arbitrary providerId.
     */
    public Task<DocumentReference> createRequest(
            String serviceId,
            String message,
            String requestedPrice,
            String priceType
    ) {

        if (auth.getCurrentUser() == null) {
            return failedTask("User is not authenticated");
        }

        if (serviceId == null || serviceId.trim().isEmpty()) {
            return failedTask("Service ID is required");
        }

        String customerId = auth.getCurrentUser().getUid();

        DocumentReference serviceRef =
                db.collection(COLLECTION_SERVICES).document(serviceId);

        DocumentReference requestRef =
                db.collection(COLLECTION_REQUESTS).document();

        return db.runTransaction(transaction -> {

            DocumentSnapshot serviceSnapshot =
                    transaction.get(serviceRef);

            if (!serviceSnapshot.exists()) {
                throw new FirebaseFirestoreException(
                        "Service does not exist",
                        FirebaseFirestoreException.Code.NOT_FOUND
                );
            }

            String providerId =
                    serviceSnapshot.getString("userId");

            if (providerId == null || providerId.trim().isEmpty()) {
                throw new FirebaseFirestoreException(
                        "Service provider is missing",
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT
                );
            }

            if (providerId.equals(customerId)) {
                throw new FirebaseFirestoreException(
                        "You cannot request your own service",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                );
            }

            Map<String, Object> data = new HashMap<>();

            data.put("requestId", requestRef.getId());
            data.put("serviceId", serviceId);
            data.put("customerId", customerId);
            data.put("providerId", providerId);

            data.put(
                    "status",
                    ServiceRequest.STATUS_REQUESTED
            );

            data.put(
                    "message",
                    message != null ? message.trim() : ""
            );

            data.put(
                    "requestedPrice",
                    requestedPrice != null
                            ? requestedPrice.trim()
                            : ""
            );

            data.put(
                    "priceType",
                    priceType != null
                            ? priceType.trim()
                            : ""
            );

            data.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            data.put(
                    "updatedAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(requestRef, data);

            return requestRef;
        });
    }

    // =========================================================
    // GET REQUEST
    // =========================================================

    public Task<DocumentSnapshot> getRequest(String requestId) {

        if (requestId == null || requestId.trim().isEmpty()) {
            return failedTask("Request ID is required");
        }

        return db.collection(COLLECTION_REQUESTS)
                .document(requestId)
                .get();
    }

    // =========================================================
    // ACCEPT
    // =========================================================

    public Task<Void> acceptRequest(String requestId) {

        return transitionRequest(
                requestId,
                ServiceRequest.STATUS_ACCEPTED,
                null
        );
    }

    // =========================================================
    // REJECT
    // =========================================================

    public Task<Void> rejectRequest(
            String requestId,
            String rejectionReason
    ) {

        return transitionRequest(
                requestId,
                ServiceRequest.STATUS_REJECTED,
                rejectionReason
        );
    }

    // =========================================================
    // START
    // =========================================================

    public Task<Void> startRequest(String requestId) {

        return transitionRequest(
                requestId,
                ServiceRequest.STATUS_IN_PROGRESS,
                null
        );
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    public Task<Void> completeRequest(String requestId) {

        return transitionRequest(
                requestId,
                ServiceRequest.STATUS_COMPLETED,
                null
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    public Task<Void> cancelRequest(
            String requestId,
            String cancellationReason
    ) {

        return transitionRequest(
                requestId,
                ServiceRequest.STATUS_CANCELLED,
                cancellationReason
        );
    }

    // =========================================================
    // STATE TRANSITION
    // =========================================================

    /**
     * Performs a request state transition atomically.
     *
     * The client validates the transition first.
     * Firestore Security Rules must independently validate:
     * - authenticated user
     * - customer/provider role
     * - allowed transition
     * - immutable identifiers
     * - timestamp integrity
     */
    private Task<Void> transitionRequest(
            String requestId,
            String newStatus,
            String reason
    ) {

        if (auth.getCurrentUser() == null) {
            return failedTask("User is not authenticated");
        }

        if (requestId == null || requestId.trim().isEmpty()) {
            return failedTask("Request ID is required");
        }

        if (newStatus == null || newStatus.trim().isEmpty()) {
            return failedTask("New status is required");
        }

        String currentUserId =
                auth.getCurrentUser().getUid();

        DocumentReference requestRef =
                db.collection(COLLECTION_REQUESTS)
                        .document(requestId);

        return db.runTransaction(transaction -> {

            DocumentSnapshot snapshot =
                    transaction.get(requestRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException(
                        "Request does not exist",
                        FirebaseFirestoreException.Code.NOT_FOUND
                );
            }

            String currentStatus =
                    snapshot.getString("status");

            String customerId =
                    snapshot.getString("customerId");

            String providerId =
                    snapshot.getString("providerId");

            if (currentStatus == null) {
                throw new FirebaseFirestoreException(
                        "Request status is missing",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                );
            }

            if (!ServiceRequest.isValidTransition(
                    currentStatus,
                    newStatus
            )) {
                throw new FirebaseFirestoreException(
                        "Invalid request status transition: "
                                + currentStatus
                                + " -> "
                                + newStatus,
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                );
            }

            boolean isCustomer =
                    currentUserId.equals(customerId);

            boolean isProvider =
                    currentUserId.equals(providerId);

            boolean allowedByClientRole = false;

            if (ServiceRequest.STATUS_ACCEPTED.equals(newStatus)
                    || ServiceRequest.STATUS_REJECTED.equals(newStatus)
                    || ServiceRequest.STATUS_IN_PROGRESS.equals(newStatus)
                    || ServiceRequest.STATUS_COMPLETED.equals(newStatus)) {

                allowedByClientRole = isProvider;
            }

            if (ServiceRequest.STATUS_CANCELLED.equals(newStatus)) {
                allowedByClientRole = isCustomer;
            }

            if (!allowedByClientRole) {
                throw new FirebaseFirestoreException(
                        "User is not allowed to perform this transition",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                );
            }

            Map<String, Object> updates =
                    new HashMap<>();

            updates.put(
                    "status",
                    newStatus
            );

            updates.put(
                    "updatedAt",
                    FieldValue.serverTimestamp()
            );

            // -------------------------------------------------
            // ACCEPTED
            // -------------------------------------------------

            if (ServiceRequest.STATUS_ACCEPTED.equals(newStatus)) {

                updates.put(
                        "acceptedAt",
                        FieldValue.serverTimestamp()
                );
            }

            // -------------------------------------------------
            // IN PROGRESS
            // -------------------------------------------------

            if (ServiceRequest.STATUS_IN_PROGRESS.equals(newStatus)) {

                updates.put(
                        "startedAt",
                        FieldValue.serverTimestamp()
                );
            }

            // -------------------------------------------------
            // COMPLETED
            // -------------------------------------------------

            if (ServiceRequest.STATUS_COMPLETED.equals(newStatus)) {

                updates.put(
                        "completedAt",
                        FieldValue.serverTimestamp()
                );
            }

            // -------------------------------------------------
            // REJECTED
            // -------------------------------------------------

            if (ServiceRequest.STATUS_REJECTED.equals(newStatus)) {

                updates.put(
                        "rejectedAt",
                        FieldValue.serverTimestamp()
                );

                if (reason != null && !reason.trim().isEmpty()) {

                    updates.put(
                            "rejectionReason",
                            reason.trim()
                    );
                }
            }

            // -------------------------------------------------
            // CANCELLED
            // -------------------------------------------------

            if (ServiceRequest.STATUS_CANCELLED.equals(newStatus)) {

                updates.put(
                        "cancelledAt",
                        FieldValue.serverTimestamp()
                );

                updates.put(
                        "cancelledBy",
                        currentUserId
                );

                if (reason != null && !reason.trim().isEmpty()) {

                    updates.put(
                            "cancellationReason",
                            reason.trim()
                    );
                }
            }

            transaction.update(
                    requestRef,
                    updates
            );

            return null;
        });
    }

    // =========================================================
    // FAILED TASK HELPER
    // =========================================================

    private <T> Task<T> failedTask(String message) {

        return com.google.android.gms.tasks.Tasks
                .forException(
                        new IllegalArgumentException(message)
                );
    }
}