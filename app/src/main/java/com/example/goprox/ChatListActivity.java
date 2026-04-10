package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private List<ChatSummary> chatList = new ArrayList<>();
    private String currentUserId;
    private Map<String, String> userNameCache = new HashMap<>();

    private final String FIREBASE_DB_URL = "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerView = findViewById(R.id.chatRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatListAdapter(chatList, chat -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("otherUserId", chat.getOtherUserId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setupBottomNavigation();
        loadChatList();
    }

    private void loadChatList() {
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("user_chats").child(currentUserId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String otherUserId = chatSnapshot.getKey();
                    String lastMsg = chatSnapshot.child("lastMessage").getValue(String.class);
                    Long ts = chatSnapshot.child("timestamp").getValue(Long.class);
                    Integer unread = chatSnapshot.child("unreadCount").getValue(Integer.class);
                    String lastMsgType = chatSnapshot.child("lastMessageType").getValue(String.class);
                    if (otherUserId == null) continue;

                    // Форматируем последнее сообщение для отображения
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
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatListActivity.this, "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        });
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
        if (userNameCache.containsKey(userId)) return userNameCache.get(userId);
        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null) {
                            userNameCache.put(userId, name);
                            updateUserNameInList(userId, name);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
        return "Loading...";
    }

    private void updateUserNameInList(String userId, String name) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getOtherUserId().equals(userId)) {
                chatList.get(i).setReceiverName(name);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
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
}