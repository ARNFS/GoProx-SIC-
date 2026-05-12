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

    private final List<ChatMessage> messages;
    private final String currentUserId;
    private MediaPlayer mediaPlayer;
    private static final int VIEW_TYPE_ME = 0;
    private static final int VIEW_TYPE_OTHER = 1;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages == null || position < 0 || position >= messages.size()) {
            return VIEW_TYPE_OTHER;
        }
        ChatMessage msg = messages.get(position);
        if (msg == null || msg.getSenderId() == null) {
            return VIEW_TYPE_OTHER;
        }
        return msg.getSenderId().equals(currentUserId) ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
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
        if (messages == null || position < 0 || position >= messages.size()) return;

        ChatMessage msg = messages.get(position);
        if (msg == null) return;

        // Time
        if (holder.tvTime != null && msg.getTimestamp() > 0) {
            holder.tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(msg.getTimestamp())));
        } else if (holder.tvTime != null) {
            holder.tvTime.setText("");
        }

        // Image message
        if ("image".equals(msg.getType())) {
            if (holder.tvMessage != null) holder.tvMessage.setVisibility(View.GONE);
            if (holder.ivImage != null) {
                holder.ivImage.setVisibility(View.VISIBLE);
                if (msg.getFileUrl() != null && !msg.getFileUrl().isEmpty()) {
                    try {
                        Glide.with(holder.itemView.getContext())
                                .load(msg.getFileUrl())
                                .placeholder(R.drawable.ic_image_placeholder)
                                .into(holder.ivImage);
                    } catch (Exception ignored) {}
                }
                holder.ivImage.setOnClickListener(v -> {
                    if (msg.getFileUrl() != null) {
                        try {
                            Intent intent = new Intent(holder.itemView.getContext(),
                                    FullscreenImageActivity.class);
                            intent.putExtra("imageUrl", msg.getFileUrl());
                            holder.itemView.getContext().startActivity(intent);
                        } catch (Exception ignored) {}
                    }
                });
            }
        }
        // File message
        else if ("file".equals(msg.getType())) {
            if (holder.tvMessage != null) {
                holder.tvMessage.setVisibility(View.VISIBLE);
                String text = msg.getText() != null ? msg.getText() : "File";
                holder.tvMessage.setText("[File] " + text);
            }
            if (holder.ivImage != null) holder.ivImage.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (holder.itemView.getContext() instanceof ChatActivity
                        && msg.getFileUrl() != null) {
                    ((ChatActivity) holder.itemView.getContext()).openFile(msg.getFileUrl());
                }
            });
        }
        // Voice message
        else if ("voice".equals(msg.getType())) {
            if (holder.tvMessage != null) {
                holder.tvMessage.setVisibility(View.VISIBLE);
                holder.tvMessage.setText("Voice message");
            }
            if (holder.ivImage != null) holder.ivImage.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> playVoiceMessage(msg.getFileUrl(), holder));
        }
        // Text message
        else {
            if (holder.tvMessage != null) {
                holder.tvMessage.setVisibility(View.VISIBLE);
                holder.tvMessage.setText(msg.getText() != null ? msg.getText() : "");
            }
            if (holder.ivImage != null) holder.ivImage.setVisibility(View.GONE);
        }
    }

    private void playVoiceMessage(String audioUrl, ViewHolder holder) {
        if (audioUrl == null || holder == null || holder.itemView == null) return;

        stopPlaying();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                try {
                    mp.start();
                    Toast.makeText(holder.itemView.getContext(),
                            "Playing voice message", Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(holder.itemView.getContext(),
                        "Failed to play audio", Toast.LENGTH_SHORT).show();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(holder.itemView.getContext(),
                    "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopPlaying() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
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