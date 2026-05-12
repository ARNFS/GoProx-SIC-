package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProfilePostsAdapter extends RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder> {

    private List<Service> postList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ProfilePostsAdapter(List<Service> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_post, parent, false);
        return new ViewHolder(view, listener);
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

        ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPostTitle);
            tvProfession = itemView.findViewById(R.id.tvPostProfession);
            tvPrice = itemView.findViewById(R.id.tvPostPrice);
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