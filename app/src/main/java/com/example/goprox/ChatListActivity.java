package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// ✅ Import ChatActivity
import com.example.goprox.ChatActivity;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatItem> chatList;
    private DatabaseReference chatsRef;
    private String userId;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chats");
        }

        recyclerView = findViewById(R.id.recyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        recyclerView.setAdapter(chatAdapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        // Add test chat immediately
        chatList.add(new ChatItem("test_chat", "Test Chat", "Send a message", System.currentTimeMillis()));
        chatAdapter.notifyDataSetChanged();

        // Load real chats from Firebase
        loadRealChats();

        setupBottomNavigation();
    }

    private void loadRealChats() {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatSnapshot.hasChild(userId)) {
                        boolean exists = false;
                        for (ChatItem item : chatList) {
                            if (item.id.equals(chatId)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                            Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);
                            if (lastMessage == null) lastMessage = "No messages";
                            if (lastMessageTime == null) lastMessageTime = System.currentTimeMillis();
                            chatList.add(new ChatItem(chatId, "Chat", lastMessage, lastMessageTime));
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Do nothing, test chat already exists
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_chats) {
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddPostActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_chats);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Adapter
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<ChatItem> chats;

        ChatAdapter(List<ChatItem> chats) {
            this.chats = chats;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatItem chat = chats.get(position);
            holder.tvName.setText(chat.name + (chat.id.equals("test_chat") ? " (test)" : ""));
            holder.tvLastMessage.setText(chat.lastMessage != null ? chat.lastMessage : "No messages");
            holder.tvTime.setText(formatTime(chat.lastMessageTime));
            holder.itemView.setOnClickListener(v -> {
                // ✅ Now ChatActivity is found
                Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                intent.putExtra("chatId", chat.id);
                intent.putExtra("otherUserId", chat.id);
                intent.putExtra("otherUserName", chat.name);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName, tvLastMessage, tvTime;

            ViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    static class ChatItem {
        String id, name, lastMessage;
        Long lastMessageTime;

        ChatItem(String id, String name, String lastMessage, Long lastMessageTime) {
            this.id = id;
            this.name = name;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
        }
    }
}