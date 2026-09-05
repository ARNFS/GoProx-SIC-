package com.example.goprox;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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
    private Handler seekHandler;
    private int playingPosition = -1;
    private SeekBar activeSeekBar;

    private static final int VIEW_TYPE_ME = 0;
    private static final int VIEW_TYPE_OTHER = 1;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.seekHandler = new Handler();
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

        // Hide all optional views
        if (holder.ivImage != null) holder.ivImage.setVisibility(View.GONE);
        if (holder.tvMessage != null) holder.tvMessage.setVisibility(View.GONE);
        if (holder.llVoice != null) holder.llVoice.setVisibility(View.GONE);
        if (holder.llFile != null) holder.llFile.setVisibility(View.GONE);
        if (holder.tvTicks != null) holder.tvTicks.setVisibility(View.GONE);
        if (holder.pbUpload != null) holder.pbUpload.setVisibility(View.GONE);
        if (holder.tvUploadProgress != null) holder.tvUploadProgress.setVisibility(View.GONE);

        // Image message
        if ("image".equals(msg.getType())) {
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
            if (holder.llFile != null) holder.llFile.setVisibility(View.VISIBLE);

            String fileName = msg.getText() != null ? msg.getText() : "File";
            String extension = getFileExtension(fileName).toUpperCase();
            String fileSize = msg.getFileSize() != null ? msg.getFileSize() : "";

            // File name
            if (holder.tvFileName != null) {
                holder.tvFileName.setText(fileName);
            }

            // Extension badge
            if (holder.tvFileExtension != null) {
                holder.tvFileExtension.setText(extension);
            }

            // File size
            if (holder.tvFileSize != null && !fileSize.isEmpty()) {
                holder.tvFileSize.setText(fileSize);
                holder.tvFileSize.setVisibility(View.VISIBLE);
            } else if (holder.tvFileSize != null) {
                holder.tvFileSize.setVisibility(View.GONE);
            }

            // File icon — միշտ file.pdf (upload-ից հետո)
            if (holder.ivFileIcon != null) {
                holder.ivFileIcon.setImageResource(R.drawable.file);
            }

            // Upload progress
            if (msg.isUploading() && holder.pbUpload != null && holder.tvUploadProgress != null) {
                holder.pbUpload.setVisibility(View.VISIBLE);
                holder.tvUploadProgress.setVisibility(View.VISIBLE);
                holder.pbUpload.setProgress(msg.getUploadProgress());
                holder.tvUploadProgress.setText(msg.getUploadProgress() + "%");
                // Թաքցնում ենք file size-ը upload-ի ժամանակ
                if (holder.tvFileSize != null) holder.tvFileSize.setVisibility(View.GONE);
            } else {
                if (holder.pbUpload != null) holder.pbUpload.setVisibility(View.GONE);
                if (holder.tvUploadProgress != null) holder.tvUploadProgress.setVisibility(View.GONE);
            }

            // Click to open
            holder.itemView.setOnClickListener(v -> {
                if (!msg.isUploading() && holder.itemView.getContext() instanceof ChatActivity
                        && msg.getFileUrl() != null) {
                    ((ChatActivity) holder.itemView.getContext()).openFile(msg.getFileUrl());
                }
            });
        }
        // Voice message
        else if ("voice".equals(msg.getType())) {
            if (holder.llVoice != null) holder.llVoice.setVisibility(View.VISIBLE);

            int duration = msg.getVoiceDuration();
            if (holder.tvDuration != null) {
                holder.tvDuration.setText(duration > 0 ? formatDuration(duration) : "0:00");
            }
            if (holder.seekBar != null) {
                holder.seekBar.setMax(duration > 0 ? duration : 100);
                holder.seekBar.setProgress(0);
            }
            if (holder.btnPlayPause != null) {
                boolean isPlaying = playingPosition == position && mediaPlayer != null && mediaPlayer.isPlaying();
                holder.btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                holder.btnPlayPause.setOnClickListener(v -> {
                    if (playingPosition == position && mediaPlayer != null && mediaPlayer.isPlaying()) {
                        pauseVoice();
                        holder.btnPlayPause.setImageResource(R.drawable.ic_play);
                    } else {
                        playVoiceMessage(msg.getFileUrl(), position, holder.seekBar,
                                holder.tvDuration, holder.btnPlayPause, duration);
                    }
                });
            }
        }
        // Text message
        else {
            if (holder.tvMessage != null) {
                holder.tvMessage.setVisibility(View.VISIBLE);
                String text = msg.getText() != null ? msg.getText() : "";
                holder.tvMessage.setLinksClickable(true);
                holder.tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.tvMessage.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
            }

            // TICKS
            if (holder.tvTicks != null && msg.getSenderId() != null
                    && msg.getSenderId().equals(currentUserId)) {
                holder.tvTicks.setVisibility(View.VISIBLE);
                if (msg.isRead()) {
                    holder.tvTicks.setText("✓✓");
                    holder.tvTicks.setTextColor(0xFF2196F3);
                } else {
                    holder.tvTicks.setText("✓");
                    holder.tvTicks.setTextColor(0xFF999999);
                }
            }
        }
    }

    // 🔥 Get file extension
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private void playVoiceMessage(String audioUrl, int position, SeekBar seekBar,
                                  TextView tvDur, ImageButton btnPlay, int duration) {
        stopPlaying();
        playingPosition = position;
        activeSeekBar = seekBar;

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                if (btnPlay != null) btnPlay.setImageResource(R.drawable.ic_pause);
                if (seekBar != null) seekBar.setMax(mp.getDuration() / 1000);
                updateSeekBar(seekBar, tvDur);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (btnPlay != null) btnPlay.setImageResource(R.drawable.ic_play);
                if (seekBar != null) seekBar.setProgress(0);
                if (tvDur != null && duration > 0) tvDur.setText(formatDuration(duration));
                stopPlaying();
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(seekBar.getContext(), "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSeekBar(SeekBar seekBar, TextView tvDur) {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && seekBar != null) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition() / 1000, true);
            if (tvDur != null) {
                int current = mediaPlayer.getCurrentPosition() / 1000;
                int total = mediaPlayer.getDuration() / 1000;
                tvDur.setText(formatDuration(current) + " / " + formatDuration(total));
            }
            seekHandler.postDelayed(() -> updateSeekBar(seekBar, tvDur), 100);
        }
    }

    private void pauseVoice() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private String formatDuration(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
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
        playingPosition = -1;
        activeSeekBar = null;
        seekHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvDuration, tvTicks;
        TextView tvFileName, tvFileExtension, tvFileSize, tvUploadProgress;
        ImageView ivImage, ivFileIcon;
        View llVoice, llFile;
        SeekBar seekBar;
        ImageButton btnPlayPause;
        ProgressBar pbUpload;

        ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTicks = itemView.findViewById(R.id.tvTicks);
            ivImage = itemView.findViewById(R.id.ivImage);
            llVoice = itemView.findViewById(R.id.llVoice);
            llFile = itemView.findViewById(R.id.llFile);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileExtension = itemView.findViewById(R.id.tvFileExtension);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            seekBar = itemView.findViewById(R.id.seekBar);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            pbUpload = itemView.findViewById(R.id.pbUpload);
            tvUploadProgress = itemView.findViewById(R.id.tvUploadProgress);
        }
    }
}