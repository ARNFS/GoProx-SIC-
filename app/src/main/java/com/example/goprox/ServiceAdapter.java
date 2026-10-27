package com.example.goprox;

import android.text.Html;
import android.text.method.LinkMovementMethod;
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
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private List<Service> serviceList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private Set<String> sponsoredServiceIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setSponsoredServiceIds(Set<String> serviceIds) {
        sponsoredServiceIds = serviceIds == null ? new HashSet<>() : new HashSet<>(serviceIds);
        notifyDataSetChanged();
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
        if (holder.tvSponsored != null) {
            boolean sponsored = service.getId() != null && sponsoredServiceIds.contains(service.getId());
            holder.tvSponsored.setVisibility(sponsored ? View.VISIBLE : View.GONE);
        }
        if (holder.tvProfession != null) {
            holder.tvProfession.setText(service.getProfession() != null ? service.getProfession() : "");
            try {
                holder.tvProfession.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
            } catch (Exception ignored) {}
        }
        if (holder.tvDescription != null) {
            String desc = service.getDescription() != null ? service.getDescription() : "";
            holder.tvDescription.setLinksClickable(true);
            holder.tvDescription.setMovementMethod(LinkMovementMethod.getInstance());
            holder.tvDescription.setText(Html.fromHtml(desc, Html.FROM_HTML_MODE_LEGACY));
            try {
                holder.tvDescription.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
            } catch (Exception ignored) {}
        }
        if (holder.tvPrice != null) {
            String price = service.getPrice() != null ? service.getPrice() : "";
            String priceType = service.getPriceType();
            holder.tvPrice.setText(
                    priceType != null && !priceType.trim().isEmpty()
                            ? price + " / " + priceType.trim()
                            : price
            );
        }
        if (holder.ratingBar != null) {
            holder.ratingBar.setRating(service.getRating());
            boolean hasVerifiedRating = service.isRatingVerified() && service.getRatingCount() > 0;
            holder.ratingBar.setVisibility(hasVerifiedRating ? View.VISIBLE : View.GONE);
            if (holder.tvRatingSummary != null) {
                holder.tvRatingSummary.setVisibility(hasVerifiedRating ? View.GONE : View.VISIBLE);
                holder.tvRatingSummary.setText(service.isRatingVerified()
                        ? "No reviews yet" : "Older rating not verified");
            }
        }

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

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(pos);
                }
            }
        });

        // 🔥 LONG CLICK
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(pos);
                    return true;
                }
            }
            return false;
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
        TextView tvName, tvProfession, tvDescription, tvPrice, tvLocation, tvSponsored, tvRatingSummary;
        RatingBar ratingBar;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivServiceImage);
            ivLocation = itemView.findViewById(R.id.ivLocation);
            tvName = itemView.findViewById(R.id.tvServiceName);
            tvSponsored = itemView.findViewById(R.id.tvSponsored);
            tvProfession = itemView.findViewById(R.id.tvServiceProfession);
            tvDescription = itemView.findViewById(R.id.tvServiceDescription);
            tvPrice = itemView.findViewById(R.id.tvServicePrice);
            tvLocation = itemView.findViewById(R.id.tvServiceLocation);
            ratingBar = itemView.findViewById(R.id.ratingBarService);
            tvRatingSummary = itemView.findViewById(R.id.tvServiceRatingSummary);
            cardView = (CardView) itemView;
        }
    }
}
