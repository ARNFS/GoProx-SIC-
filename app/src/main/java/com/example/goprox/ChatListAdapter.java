package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<ChatSummary> chatList;
    private final OnChatClickListener listener;

    private final String FIREBASE_DB_URL = "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app";

    public interface OnChatClickListener {
        void onChatClick(ChatSummary chat);
    }

    public ChatListAdapter(List<ChatSummary> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        if (chatList == null || position < 0 || position >= chatList.size()) return;

        ChatSummary chat = chatList.get(position);
        if (chat == null) return;

        // 🔥 Profile photo — load from Firebase
        if (holder.ivAvatar != null) {
            loadProfilePhoto(chat.getOtherUserId(), holder.ivAvatar);
        }

        // Name
        if (holder.nameText != null) {
            String cachedName = chat.getReceiverName();
            if (cachedName != null && !cachedName.equals("Loading...") && !cachedName.equals("Unknown") && !cachedName.equals("User")) {
                holder.nameText.setText(cachedName);
            } else {
                holder.nameText.setText("Loading...");
                loadUserName(chat.getOtherUserId(), holder.nameText, chat);
            }
        }

        // Last message
        if (holder.lastMessageText != null) {
            holder.lastMessageText.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");
        }

        // Time
        if (holder.timeText != null) {
            if (chat.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                holder.timeText.setText(sdf.format(new Date(chat.getTimestamp())));
            } else {
                holder.timeText.setText("");
            }
        }

        // Unread badge
        int unread = Math.max(chat.getUnreadCount(), 0);
        if (holder.unreadBadge != null) {
            if (unread > 0) {
                holder.unreadBadge.setVisibility(View.VISIBLE);
                holder.unreadBadge.setText(String.valueOf(unread));
            } else {
                holder.unreadBadge.setVisibility(View.GONE);
            }
        }

        // Name color based on unread
        if (holder.nameText != null && holder.itemView != null) {
            try {
                holder.nameText.setTextColor(unread > 0
                        ? ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black)
                        : ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
            } catch (Exception ignored) {}
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && chat != null) {
                listener.onChatClick(chat);
            }
        });
    }

    // 🔥 LOAD PROFILE PHOTO
    private void loadProfilePhoto(String userId, CircleImageView ivAvatar) {
        if (userId == null || ivAvatar == null) return;

        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("users").child(userId).child("photoUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String photoUrl = snapshot.getValue(String.class);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(ivAvatar.getContext())
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(ivAvatar);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadUserName(String userId, TextView tvName, ChatSummary chat) { /* ... */ }
    private void loadUserEmail(String userId, TextView tvName, ChatSummary chat) { /* ... */ }

    @Override
    public int getItemCount() { return chatList != null ? chatList.size() : 0; }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView nameText, lastMessageText, timeText, unreadBadge;

        ChatViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            nameText = itemView.findViewById(R.id.text_user_name);
            lastMessageText = itemView.findViewById(R.id.text_last_message);
            timeText = itemView.findViewById(R.id.text_time);
            unreadBadge = itemView.findViewById(R.id.unread_badge);
        }
    }
}