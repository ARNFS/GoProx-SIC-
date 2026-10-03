package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServiceRequestAdapter
        extends RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder> {

    public interface OnRequestClickListener {
        void onRequestClick(ServiceRequest request);
    }

    private final List<ServiceRequest> requests;
    private final Map<String, String> serviceNames;
    private final Map<String, String> serviceImages;
    private final Map<String, String> userNames;
    private final Map<String, String> userPhotos;
    private final OnRequestClickListener listener;

    public ServiceRequestAdapter(
            List<ServiceRequest> requests,
            Map<String, String> serviceNames,
            Map<String, String> serviceImages,
            Map<String, String> userNames,
            Map<String, String> userPhotos,
            OnRequestClickListener listener
    ) {
        this.requests = requests;
        this.serviceNames = serviceNames;
        this.serviceImages = serviceImages;
        this.userNames = userNames;
        this.userPhotos = userPhotos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_service_request,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        ServiceRequest request =
                requests.get(position);

        String serviceId =
                request.getServiceId();

        String otherUserId =
                getOtherUserId(request);

        String serviceName =
                serviceNames.get(serviceId);

        String userName =
                userNames.get(otherUserId);

        String serviceImage =
                serviceImages.get(serviceId);

        String userPhoto =
                userPhotos.get(otherUserId);

        holder.tvServiceName.setText(
                serviceName != null
                        ? serviceName
                        : "Service"
        );

        holder.tvUserName.setText(
                userName != null
                        ? userName
                        : "User"
        );

        String message = request.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            holder.tvMessage.setText(
                    "No message"
            );

        } else {

            holder.tvMessage.setText(
                    "“" + message.trim() + "”"
            );
        }

        String price =
                request.getRequestedPrice();

        String priceType =
                request.getPriceType();

        if (price == null || price.trim().isEmpty()) {
            holder.tvPrice.setText(
                    "Price not specified"
            );
        } else {

            String formattedPrice =
                    price.trim() + " ֏";

            if (priceType != null
                    && !priceType.trim().isEmpty()) {

                formattedPrice +=
                        " · " + priceType.trim();
            }

            holder.tvPrice.setText(
                    formattedPrice
            );
        }

        holder.tvStatus.setText(
                getStatusText(request.getStatus())
        );

        holder.tvStatus.setTextColor(
                holder.itemView.getContext()
                        .getColor(
                                getStatusColor(
                                        request.getStatus()
                                )
                        )
        );

        holder.tvDate.setText(
                formatTimestamp(
                        request.getUpdatedAt(),
                        request.getCreatedAt()
                )
        );

        Glide.with(holder.itemView.getContext())
                .load(
                        userPhoto != null
                                && !userPhoto.isEmpty()
                                ? userPhoto
                                : R.drawable.ic_profile_placeholder
                )
                .placeholder(
                        R.drawable.ic_profile_placeholder
                )
                .error(
                        R.drawable.ic_profile_placeholder
                )
                .circleCrop()
                .into(holder.ivAvatar);

        holder.card.setOnClickListener(
                v -> {
                    if (listener != null) {
                        listener.onRequestClick(
                                request
                        );
                    }
                }
        );
    }

    private String getOtherUserId(
            ServiceRequest request
    ) {
        if (request == null) {
            return null;
        }

        // The adapter doesn't know the current user directly.
        // Prefer customer as the visible party for incoming
        // requests and provider for sent requests.
        //
        // The Activity's maps contain only the relevant user.
        if (userNames.containsKey(
                request.getCustomerId()
        )) {
            return request.getCustomerId();
        }

        return request.getProviderId();
    }

    private String getStatusText(String status) {

        if (status == null) {
            return "Unknown";
        }

        switch (status) {

            case ServiceRequest.STATUS_REQUESTED:
                return "Pending";

            case ServiceRequest.STATUS_ACCEPTED:
                return "Accepted";

            case ServiceRequest.STATUS_IN_PROGRESS:
                return "In progress";

            case ServiceRequest.STATUS_COMPLETED:
                return "Completed";

            case ServiceRequest.STATUS_REJECTED:
                return "Rejected";

            case ServiceRequest.STATUS_CANCELLED:
                return "Cancelled";

            default:
                return "Unknown";
        }
    }

    private int getStatusColor(String status) {

        if (status == null) {
            return R.color.gray;
        }

        switch (status) {

            case ServiceRequest.STATUS_REQUESTED:
                return R.color.yellow;

            case ServiceRequest.STATUS_ACCEPTED:
                return R.color.blue;

            case ServiceRequest.STATUS_IN_PROGRESS:
                return R.color.primary;

            case ServiceRequest.STATUS_COMPLETED:
                return R.color.green;

            case ServiceRequest.STATUS_REJECTED:
            case ServiceRequest.STATUS_CANCELLED:
                return R.color.red;

            default:
                return R.color.gray;
        }
    }

    private String formatTimestamp(
            com.google.firebase.Timestamp updatedAt,
            com.google.firebase.Timestamp createdAt
    ) {
        com.google.firebase.Timestamp timestamp =
                updatedAt != null
                        ? updatedAt
                        : createdAt;

        if (timestamp == null) {
            return "Recently";
        }

        Date date =
                timestamp.toDate();

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "MMM d · HH:mm",
                        Locale.getDefault()
                );

        return format.format(date);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView card;

        ImageView ivAvatar;

        TextView tvServiceName;
        TextView tvUserName;
        TextView tvMessage;
        TextView tvPrice;
        TextView tvStatus;
        TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            card = itemView.findViewById(
                    R.id.cardRequest
            );

            ivAvatar = itemView.findViewById(
                    R.id.ivAvatar
            );

            tvServiceName = itemView.findViewById(
                    R.id.tvServiceName
            );

            tvUserName = itemView.findViewById(
                    R.id.tvUserName
            );

            tvMessage = itemView.findViewById(
                    R.id.tvMessage
            );

            tvPrice = itemView.findViewById(
                    R.id.tvPrice
            );

            tvStatus = itemView.findViewById(
                    R.id.tvStatus
            );

            tvDate = itemView.findViewById(
                    R.id.tvDate
            );
        }
    }
}