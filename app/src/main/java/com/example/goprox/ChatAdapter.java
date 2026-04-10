package com.example.goprox;

import android.content.Intent;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> messages;
    private String currentUserId;
    private MediaPlayer mediaPlayer;
    private static final int VIEW_TYPE_ME = 0;
    private static final int VIEW_TYPE_OTHER = 1;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUserId) ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_ME) {
            view = inflater.inflate(R.layout.item_chat_me, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_chat_other, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(msg.getTimestamp())));

        if ("image".equals(msg.getType())) {
            holder.tvMessage.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(msg.getFileUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.ivImage);
            holder.ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), FullscreenImageActivity.class);
                intent.putExtra("imageUrl", msg.getFileUrl());
                holder.itemView.getContext().startActivity(intent);
            });
        } else if ("file".equals(msg.getType())) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivImage.setVisibility(View.GONE);
            holder.tvMessage.setText("[File] " + msg.getText());
            holder.itemView.setOnClickListener(v -> {
                if (holder.itemView.getContext() instanceof ChatActivity) {
                    ((ChatActivity) holder.itemView.getContext()).openFile(msg.getFileUrl());
                }
            });
        } else if ("voice".equals(msg.getType())) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivImage.setVisibility(View.GONE);
            holder.tvMessage.setText("Voice message");
            holder.itemView.setOnClickListener(v -> playVoiceMessage(msg.getFileUrl(), holder));
        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivImage.setVisibility(View.GONE);
            holder.tvMessage.setText(msg.getText());
        }
    }

    private void playVoiceMessage(String audioUrl, ViewHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                Toast.makeText(holder.itemView.getContext(), "Playing voice message", Toast.LENGTH_SHORT).show();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(holder.itemView.getContext(), "Failed to play audio", Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (IOException e) {
            Toast.makeText(holder.itemView.getContext(), "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;

        ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);
        }
    }
}