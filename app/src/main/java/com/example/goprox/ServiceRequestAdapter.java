package com.example.goprox;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ServiceRequestAdapter
        extends RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder> {

    public interface OnRequestClickListener {
        void onRequestClick(ServiceRequest request);
    }

    private final List<ServiceRequest> requestList;
    private final OnRequestClickListener listener;

    private boolean incomingMode;

    public ServiceRequestAdapter(
            List<ServiceRequest> requestList,
            boolean incomingMode,
            OnRequestClickListener listener
    ) {
        this.requestList = requestList;
        this.incomingMode = incomingMode;
        this.listener = listener;
    }

    public void setIncomingMode(boolean incomingMode) {
        this.incomingMode = incomingMode;
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

        if (position < 0
                || position >= requestList.size()) {
            return;
        }

        ServiceRequest request =
                requestList.get(position);

        if (request == null) return;

        String status = request.getStatus();

        holder.tvServiceTitle.setText(
                "Service Request"
        );

        String personText;

        if (incomingMode) {
            personText =
                    "Customer: "
                            + safeId(request.getCustomerId());
        } else {
            personText =
                    "Provider: "
                            + safeId(request.getProviderId());
        }

        holder.tvPerson.setText(personText);

        String message = request.getMessage();

        if (message == null || message.trim().isEmpty()) {
            holder.tvMessage.setText(
                    "No message provided"
            );
        } else {
            holder.tvMessage.setText(
                    "“" + message.trim() + "”"
            );
        }

        String price =
                request.getRequestedPrice();

        if (price == null || price.trim().isEmpty()) {
            price = "Price not specified";
        } else {
            String priceType =
                    request.getPriceType();

            if (priceType != null
                    && !priceType.trim().isEmpty()) {
                price += " / " + priceType;
            }
        }

        holder.tvPrice.setText(price);

        holder.tvStatus.setText(
                getStatusLabel(status)
        );

        int statusColor =
                getStatusColor(status);

        holder.tvStatus.setTextColor(
                statusColor
        );

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onRequestClick(request);
            }
        });
    }

    private String safeId(String id) {

        if (id == null || id.trim().isEmpty()) {
            return "Unknown";
        }

        String value = id.trim();

        if (value.length() <= 12) {
            return value;
        }

        return value.substring(0, 12) + "…";
    }

    private String getStatusLabel(String status) {

        if (ServiceRequest.STATUS_REQUESTED.equals(status)) {
            return "● Waiting for response";
        }

        if (ServiceRequest.STATUS_ACCEPTED.equals(status)) {
            return "● Accepted";
        }

        if (ServiceRequest.STATUS_IN_PROGRESS.equals(status)) {
            return "● In progress";
        }

        if (ServiceRequest.STATUS_COMPLETED.equals(status)) {
            return "● Completed";
        }

        if (ServiceRequest.STATUS_REJECTED.equals(status)) {
            return "● Rejected";
        }

        if (ServiceRequest.STATUS_CANCELLED.equals(status)) {
            return "● Cancelled";
        }

        return "● Unknown";
    }

    private int getStatusColor(String status) {

        if (ServiceRequest.STATUS_REQUESTED.equals(status)) {
            return Color.rgb(249, 168, 37);
        }

        if (ServiceRequest.STATUS_ACCEPTED.equals(status)) {
            return Color.rgb(21, 101, 192);
        }

        if (ServiceRequest.STATUS_IN_PROGRESS.equals(status)) {
            return Color.rgb(21, 101, 192);
        }

        if (ServiceRequest.STATUS_COMPLETED.equals(status)) {
            return Color.rgb(46, 125, 50);
        }

        if (ServiceRequest.STATUS_REJECTED.equals(status)) {
            return Color.rgb(198, 40, 40);
        }

        if (ServiceRequest.STATUS_CANCELLED.equals(status)) {
            return Color.rgb(97, 97, 97);
        }

        return Color.rgb(97, 97, 97);
    }

    @Override
    public int getItemCount() {
        return requestList != null
                ? requestList.size()
                : 0;
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvServiceTitle;
        TextView tvPerson;
        TextView tvMessage;
        TextView tvPrice;
        TextView tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvServiceTitle =
                    itemView.findViewById(
                            R.id.tvRequestServiceTitle
                    );

            tvPerson =
                    itemView.findViewById(
                            R.id.tvRequestPerson
                    );

            tvMessage =
                    itemView.findViewById(
                            R.id.tvRequestMessage
                    );

            tvPrice =
                    itemView.findViewById(
                            R.id.tvRequestPrice
                    );

            tvStatus =
                    itemView.findViewById(
                            R.id.tvRequestStatus
                    );
        }
    }
}