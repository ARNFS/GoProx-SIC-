package com.example.goprox;

import com.google.firebase.Timestamp;

public class ServiceRequest {

    // =========================================================
    // STATUS
    // =========================================================

    public static final String STATUS_REQUESTED = "requested";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";

    // =========================================================
    // CORE IDENTIFIERS
    // =========================================================

    private String requestId;
    private String serviceId;

    private String customerId;
    private String providerId;

    // =========================================================
    // REQUEST DATA
    // =========================================================

    private String status;

    private String message;
    private String requestedPrice;
    private String priceType;

    // =========================================================
    // TIMESTAMPS
    // =========================================================

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private Timestamp acceptedAt;
    private Timestamp startedAt;
    private Timestamp completedAt;

    private Timestamp rejectedAt;
    private Timestamp cancelledAt;

    // =========================================================
    // CANCELLATION / REJECTION
    // =========================================================

    private String cancelledBy;
    private String rejectionReason;
    private String cancellationReason;

    // =========================================================
    // FIRESTORE
    // =========================================================

    public ServiceRequest() {
        // Required by Firestore
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ServiceRequest(
            String requestId,
            String serviceId,
            String customerId,
            String providerId,
            String status,
            String message,
            String requestedPrice,
            String priceType
    ) {
        this.requestId = requestId;
        this.serviceId = serviceId;
        this.customerId = customerId;
        this.providerId = providerId;
        this.status = status;
        this.message = message;
        this.requestedPrice = requestedPrice;
        this.priceType = priceType;
    }

    // =========================================================
    // STATE MACHINE
    // =========================================================

    /**
     * Checks whether a request can move from one status to another.
     *
     * This is the client-side business-rule layer.
     * Firestore Security Rules must enforce the same transitions
     * on the server side.
     */
    public static boolean isValidTransition(String currentStatus, String newStatus) {

        if (currentStatus == null || newStatus == null) {
            return false;
        }

        if (currentStatus.equals(newStatus)) {
            return false;
        }

        switch (currentStatus) {

            case STATUS_REQUESTED:
                return newStatus.equals(STATUS_ACCEPTED)
                        || newStatus.equals(STATUS_REJECTED)
                        || newStatus.equals(STATUS_CANCELLED);

            case STATUS_ACCEPTED:
                return newStatus.equals(STATUS_IN_PROGRESS)
                        || newStatus.equals(STATUS_CANCELLED);

            case STATUS_IN_PROGRESS:
                return newStatus.equals(STATUS_COMPLETED)
                        || newStatus.equals(STATUS_CANCELLED);

            case STATUS_COMPLETED:
            case STATUS_REJECTED:
            case STATUS_CANCELLED:
                return false;

            default:
                return false;
        }
    }

    /**
     * Returns whether this request is in a terminal state.
     *
     * Terminal states cannot transition to another state.
     */
    public boolean isTerminal() {

        return STATUS_COMPLETED.equals(status)
                || STATUS_REJECTED.equals(status)
                || STATUS_CANCELLED.equals(status);
    }

    /**
     * Returns whether the customer is allowed to cancel
     * the request according to the current client-side state.
     *
     * Final permission will also be enforced by Firestore Rules.
     */
    public boolean canCustomerCancel() {

        return STATUS_REQUESTED.equals(status)
                || STATUS_ACCEPTED.equals(status)
                || STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Returns whether the provider can accept the request.
     */
    public boolean canProviderAccept() {

        return STATUS_REQUESTED.equals(status);
    }

    /**
     * Returns whether the provider can reject the request.
     */
    public boolean canProviderReject() {

        return STATUS_REQUESTED.equals(status);
    }

    /**
     * Returns whether the provider can start the service.
     */
    public boolean canProviderStart() {

        return STATUS_ACCEPTED.equals(status);
    }

    /**
     * Returns whether the provider can complete the service.
     */
    public boolean canProviderComplete() {

        return STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Returns whether the customer is potentially eligible
     * to leave a review.
     *
     * Actual review eligibility will later be verified against
     * the Firestore request document.
     */
    public boolean isReviewEligible() {

        return STATUS_COMPLETED.equals(status);
    }

    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestedPrice() {
        return requestedPrice;
    }

    public void setRequestedPrice(String requestedPrice) {
        this.requestedPrice = requestedPrice;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Timestamp acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Timestamp rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}