package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = serviceList.get(position);
        holder.tvName.setText(service.getName());
        holder.tvProfession.setText(service.getProfession());
        holder.tvDescription.setText(service.getDescription());
        holder.tvPrice.setText(service.getPrice());
        holder.ratingBar.setRating(service.getRating());

        // Цвета
        holder.tvProfession.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
        holder.tvDescription.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));

        // Аватар
        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(service.getImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Локация
        String country = service.getCountry();
        String city = service.getCity();
        if (country != null && !country.isEmpty() && city != null && !city.isEmpty()) {
            holder.tvLocation.setText(city + ", " + country);
            holder.ivLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else if (country != null && !country.isEmpty()) {
            holder.tvLocation.setText(country);
            holder.ivLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else if (city != null && !city.isEmpty()) {
            holder.tvLocation.setText(city);
            holder.ivLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.ivLocation.setVisibility(View.GONE);
            holder.tvLocation.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void updateList(List<Service> newList) {
        serviceList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, ivLocation;
        TextView tvName, tvProfession, tvDescription, tvPrice, tvLocation;
        RatingBar ratingBar;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivServiceImage);
            ivLocation = itemView.findViewById(R.id.ivLocation);
            tvName = itemView.findViewById(R.id.tvServiceName);
            tvProfession = itemView.findViewById(R.id.tvServiceProfession);
            tvDescription = itemView.findViewById(R.id.tvServiceDescription);
            tvPrice = itemView.findViewById(R.id.tvServicePrice);
            tvLocation = itemView.findViewById(R.id.tvServiceLocation);
            ratingBar = itemView.findViewById(R.id.ratingBarService);
            cardView = (CardView) itemView;
        }
    }
}