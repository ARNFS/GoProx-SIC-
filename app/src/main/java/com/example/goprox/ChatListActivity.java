package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private final List<ChatSummary> chatList = new ArrayList<>();
    private String currentUserId;
    private final Map<String, String> userNameCache = new HashMap<>();
    private ValueEventListener chatListListener;
    private DatabaseReference chatRef;

    private final String FIREBASE_DB_URL = "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerView = findViewById(R.id.chatRecyclerView);
        if (recyclerView == null) {
            Toast.makeText(this, "UI error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatListAdapter(chatList, chat -> {
            if (chat != null && chat.getOtherUserId() != null) {
                Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                intent.putExtra("otherUserId", chat.getOtherUserId());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();
        setupBottomNavigation();
        loadChatList();
    }

    private void loadChatList() {
        if (currentUserId == null) return;

        chatRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("user_chats").child(currentUserId);

        chatListListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                synchronized (chatList) {
                    chatList.clear();
                    for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                        String otherUserId = chatSnapshot.getKey();
                        if (otherUserId == null) continue;

                        String lastMsg = chatSnapshot.child("lastMessage").getValue(String.class);
                        Long ts = chatSnapshot.child("timestamp").getValue(Long.class);
                        Integer unread = chatSnapshot.child("unreadCount").getValue(Integer.class);
                        String lastMsgType = chatSnapshot.child("lastMessageType").getValue(String.class);

                        String displayMessage = formatLastMessage(lastMsg, lastMsgType);
                        ChatSummary cs = new ChatSummary(
                                otherUserId,
                                getUserName(otherUserId),
                                displayMessage,
                                ts != null ? ts : 0L,
                                unread != null ? unread : 0
                        );
                        chatList.add(cs);
                    }
                    Collections.sort(chatList, (a, b) -> {
                        if (a.getUnreadCount() > 0 && b.getUnreadCount() == 0) return -1;
                        if (a.getUnreadCount() == 0 && b.getUnreadCount() > 0) return 1;
                        return Long.compare(b.getTimestamp(), a.getTimestamp());
                    });
                }
                runOnUiThread(() -> {
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatListActivity.this, "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        };
        chatRef.addValueEventListener(chatListListener);
    }

    private String formatLastMessage(String content, String type) {
        if (content == null) return "";
        if (type == null) return content;
        switch (type) {
            case "image":
                return "Photo";
            case "file":
                return "[File] " + content;
            case "voice":
                return "Voice message";
            default:
                return content;
        }
    }

    private String getUserName(String userId) {
        if (userId == null) return "Unknown";
        synchronized (userNameCache) {
            if (userNameCache.containsKey(userId)) {
                String cached = userNameCache.get(userId);
                if (cached != null) return cached;
            }
        }

        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null) {
                            synchronized (userNameCache) {
                                userNameCache.put(userId, name);
                            }
                            updateUserNameInList(userId, name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        return "Loading...";
    }

    private void updateUserNameInList(String userId, String name) {
        if (userId == null || name == null || adapter == null) return;
        synchronized (chatList) {
            for (int i = 0; i < chatList.size(); i++) {
                ChatSummary cs = chatList.get(i);
                if (cs != null && userId.equals(cs.getOtherUserId())) {
                    cs.setReceiverName(name);
                    final int index = i;
                    runOnUiThread(() -> adapter.notifyItemChanged(index));
                    break;
                }
            }
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav == null) return;

        nav.setOnNavigationItemSelectedListener(item -> {
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
        nav.setSelectedItemId(R.id.nav_chats);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && chatListListener != null) {
            try {
                chatRef.removeEventListener(chatListListener);
            } catch (Exception ignored) {}
        }
        chatListListener = null;
        chatRef = null;
    }
}