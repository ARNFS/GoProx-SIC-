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
        if (serviceList == null || position < 0 || position >= serviceList.size()) return;

        Service service = serviceList.get(position);
        if (service == null) return;

        if (holder.tvName != null) {
            holder.tvName.setText(service.getName() != null ? service.getName() : "");
        }
        if (holder.tvProfession != null) {
            holder.tvProfession.setText(service.getProfession() != null ? service.getProfession() : "");
            try {
                holder.tvProfession.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
            } catch (Exception ignored) {}
        }
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(service.getDescription() != null ? service.getDescription() : "");
            try {
                holder.tvDescription.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
            } catch (Exception ignored) {}
        }
        if (holder.tvPrice != null) {
            holder.tvPrice.setText(service.getPrice() != null ? service.getPrice() : "");
        }
        if (holder.ratingBar != null) {
            holder.ratingBar.setRating(service.getRating());
        }

        // Avatar
        if (holder.ivProfile != null) {
            if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
                try {
                    Glide.with(holder.itemView.getContext())
                            .load(service.getImageUrl())
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(holder.ivProfile);
                } catch (Exception e) {
                    holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
                }
            } else {
                holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

        // Location
        String country = service.getCountry();
        String city = service.getCity();
        boolean hasCountry = country != null && !country.isEmpty();
        boolean hasCity = city != null && !city.isEmpty();

        if (holder.tvLocation != null && holder.ivLocation != null) {
            if (hasCountry && hasCity) {
                holder.tvLocation.setText(city + ", " + country);
                holder.ivLocation.setVisibility(View.VISIBLE);
                holder.tvLocation.setVisibility(View.VISIBLE);
            } else if (hasCountry) {
                holder.tvLocation.setText(country);
                holder.ivLocation.setVisibility(View.VISIBLE);
                holder.tvLocation.setVisibility(View.VISIBLE);
            } else if (hasCity) {
                holder.tvLocation.setText(city);
                holder.ivLocation.setVisibility(View.VISIBLE);
                holder.tvLocation.setVisibility(View.VISIBLE);
            } else {
                holder.ivLocation.setVisibility(View.GONE);
                holder.tvLocation.setVisibility(View.GONE);
            }
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
        return serviceList != null ? serviceList.size() : 0;
    }

    public void updateList(List<Service> newList) {
        this.serviceList = newList;
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