package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfilePostsAdapter extends RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder> {

    private List<Service> postList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnPromotionClickListener promotionClickListener;
    private Set<String> promotedServiceIds = new HashSet<>();
    private boolean promotionStateKnown;
    private boolean promotionStateUnavailable;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnPromotionClickListener {
        void onPromotionClick(Service service, boolean currentlyPromoted);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setOnPromotionClickListener(OnPromotionClickListener listener) {
        this.promotionClickListener = listener;
    }

    public void setPromotedServiceIds(Set<String> serviceIds) {
        this.promotedServiceIds = serviceIds == null ? new HashSet<>() : new HashSet<>(serviceIds);
        promotionStateKnown = true;
        promotionStateUnavailable = false;
        notifyDataSetChanged();
    }

    public void setPromotionStateUnknown() {
        promotionStateKnown = false;
        promotionStateUnavailable = false;
        notifyDataSetChanged();
    }

    public void setPromotionStateUnavailable() {
        promotionStateKnown = false;
        promotionStateUnavailable = true;
        notifyDataSetChanged();
    }

    public ProfilePostsAdapter(List<Service> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_post, parent, false);
        return new ViewHolder(view, listener, longClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (postList == null || position < 0 || position >= postList.size()) return;

        Service post = postList.get(position);
        if (post == null) return;

        if (holder.tvTitle != null) {
            holder.tvTitle.setText(post.getName() != null ? post.getName() : "");
        }
        if (holder.tvProfession != null) {
            holder.tvProfession.setText(post.getProfession() != null ? post.getProfession() : "");
        }
        if (holder.tvPrice != null) {
            holder.tvPrice.setText(post.getPrice() != null ? post.getPrice() : "");
        }
        boolean promoted = post.getId() != null && promotedServiceIds.contains(post.getId());
        if (holder.btnPromotion != null) {
            holder.btnPromotion.setText(promotionStateKnown
                    ? promoted ? "Stop promotion" : "Promote"
                    : promotionStateUnavailable ? "Unavailable" : "Checking…");
            holder.btnPromotion.setEnabled(promotionStateKnown);
            holder.btnPromotion.setOnClickListener(v -> {
                if (promotionClickListener != null) {
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < postList.size()) {
                        Service selected = postList.get(adapterPosition);
                        promotionClickListener.onPromotionClick(selected,
                                selected != null && selected.getId() != null
                                        && promotedServiceIds.contains(selected.getId()));
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public void updateList(List<Service> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvProfession, tvPrice;
        CardView cardView;

        Button btnPromotion;

        ViewHolder(@NonNull View itemView, OnItemClickListener clickListener,
                   OnItemLongClickListener longClickListener) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPostTitle);
            tvProfession = itemView.findViewById(R.id.tvPostProfession);
            tvPrice = itemView.findViewById(R.id.tvPostPrice);
            btnPromotion = itemView.findViewById(R.id.btnPostPromotion);
            cardView = itemView.findViewById(R.id.cardView);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onItemClick(position);
                    }
                }
            });

            // 🔥 LONG CLICK
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longClickListener.onItemLongClick(position);
                        return true;
                    }
                }
                return false;
            });
        }
    }
}
