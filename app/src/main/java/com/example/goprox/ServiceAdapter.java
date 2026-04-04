package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private List<Service> serviceList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ServiceAdapter(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = serviceList.get(position);

        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(service.getImageUrl())
                    .placeholder(service.getImageResId())
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(service.getImageResId());
        }

        holder.tvName.setText(service.getName());
        holder.tvProfession.setText(service.getProfession());
        holder.tvDescription.setText(service.getDescription());
        holder.tvPrice.setText(service.getPrice());
        holder.ratingBar.setRating(service.getRating());
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void updateList(List<Service> newList) {
        this.serviceList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvProfession, tvDescription, tvPrice;
        RatingBar ratingBar;
        CardView cardView;

        public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvProfession = itemView.findViewById(R.id.tvProfession);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            cardView = itemView.findViewById(R.id.cardView);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}