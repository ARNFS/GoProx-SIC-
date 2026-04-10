package com.example.goprox;

import android.content.Context;
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

public class SpecialistAdapter extends RecyclerView.Adapter<SpecialistAdapter.ViewHolder> {

    private Context context;
    private List<Service> serviceList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public SpecialistAdapter(Context context, List<Service> serviceList) {
        this.context = context;
        this.serviceList = serviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service, parent, false);
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

        // КРАСНЫЙ цвет профессии
        holder.tvProfession.setTextColor(ContextCompat.getColor(context, R.color.red));

        // Синий цвет описания (остаётся)
        holder.tvDescription.setTextColor(ContextCompat.getColor(context, R.color.blue));

        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(service.getImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
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
        ImageView ivProfile;
        TextView tvName, tvProfession, tvDescription, tvPrice;
        RatingBar ratingBar;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivServiceImage);
            tvName = itemView.findViewById(R.id.tvServiceName);
            tvProfession = itemView.findViewById(R.id.tvServiceProfession);
            tvDescription = itemView.findViewById(R.id.tvServiceDescription);
            tvPrice = itemView.findViewById(R.id.tvServicePrice);
            ratingBar = itemView.findViewById(R.id.ratingBarService);
            cardView = (CardView) itemView;
        }
    }
}