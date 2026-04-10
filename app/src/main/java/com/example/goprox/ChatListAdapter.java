package com.example.goprox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatSummary> chatList;
    private OnChatClickListener listener;

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
        ChatSummary chat = chatList.get(position);
        holder.nameText.setText(chat.getReceiverName());
        holder.lastMessageText.setText(chat.getLastMessage());

        if (chat.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.timeText.setText(sdf.format(new Date(chat.getTimestamp())));
        } else {
            holder.timeText.setText("");
        }

        // Unread badge
        int unread = chat.getUnreadCount();
        if (unread > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(String.valueOf(unread));
            holder.nameText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
            holder.nameText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, lastMessageText, timeText, unreadBadge;

        ChatViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_user_name);
            lastMessageText = itemView.findViewById(R.id.text_last_message);
            timeText = itemView.findViewById(R.id.text_time);
            unreadBadge = itemView.findViewById(R.id.unread_badge);
        }
    }
}