package com.example.goprox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private EditText etMessage;
    private ImageView btnSend;
    private ImageView btnAttach;
    private ImageView btnVoiceCall;
    private ImageView btnVideoCall;
    private LinearLayout llMessages;
    private ScrollView scrollView;

    private DatabaseReference chatRef;
    private DatabaseReference chatMetaRef;
    private StorageReference storageRef;
    private String chatId;
    private String userId;
    private String otherUserId;
    private String otherUserName;

    private static final int PICK_FILE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST_STORAGE = 1002;
    private static final int PERMISSION_REQUEST_MANAGE_STORAGE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        btnVoiceCall = findViewById(R.id.btnVoiceCall);
        btnVideoCall = findViewById(R.id.btnVideoCall);
        llMessages = findViewById(R.id.llMessages);
        scrollView = findViewById(R.id.scrollView);

        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");

        if (chatId == null) chatId = "test_chat";
        if (otherUserId == null) otherUserId = "unknown";
        if (otherUserName == null) otherUserName = "User";

        getSupportActionBar().setTitle(otherUserName);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        chatMetaRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
        storageRef = FirebaseStorage.getInstance().getReference();

        updateChatMetadata("Chat started");
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());

        // ✅ Null checks for buttons
        if (btnAttach != null) btnAttach.setOnClickListener(v -> requestStoragePermission());
        if (btnVoiceCall != null) btnVoiceCall.setOnClickListener(v -> startVoiceCall());
        if (btnVideoCall != null) btnVideoCall.setOnClickListener(v -> startVideoCall());

        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                new Handler().postDelayed(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN), 150);
            }
        });
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                openFileChooser();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, PERMISSION_REQUEST_MANAGE_STORAGE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_STORAGE);
            } else {
                openFileChooser();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                openFileChooser();
            } else {
                Toast.makeText(this, "Manage storage permission required", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    private void uploadFile(Uri fileUri) {
        if (fileUri == null) {
            Toast.makeText(this, "Invalid file", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = UUID.randomUUID().toString();
        StorageReference fileRef = storageRef.child("chat_files/" + fileName);

        String mimeType = getContentResolver().getType(fileUri);
        boolean isImage = mimeType != null && mimeType.startsWith("image/");

        if (isImage) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                int maxSize = 1600;
                int sampleSize = 1;
                while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
                    sampleSize *= 2;
                }
                options.inSampleSize = sampleSize;
                options.inJustDecodeBounds = false;

                inputStream = getContentResolver().openInputStream(fileUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] data = baos.toByteArray();

                fileRef.putBytes(data)
                        .addOnSuccessListener(taskSnapshot -> {
                            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                sendFileMessage(uri.toString(), fileName, mimeType, true);
                            }).addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to get URL: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Toast.makeText(this, "Image processing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            fileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            sendFileMessage(uri.toString(), fileName, mimeType, false);
                        }).addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to get URL: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void sendFileMessage(String fileUrl, String fileName, String mimeType, boolean isImage) {
        String timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String messageId = chatRef.push().getKey();
        String type = isImage ? "image" : "file";
        String displayText = isImage ? "📷 Image" : "📎 " + fileName;
        Message message = new Message(userId, displayText, timestamp, type, fileUrl, mimeType, fileName);
        chatRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    scrollToBottom();
                    updateChatMetadata(displayText);
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        String timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String messageId = chatRef.push().getKey();
        Message message = new Message(userId, text, timestamp, "text", null, null, null);
        chatRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                    scrollToBottom();
                    updateChatMetadata(text);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }

    private void updateChatMetadata(String lastMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTime", ServerValue.TIMESTAMP);
        chatMetaRef.updateChildren(updates);
    }

    private void startVoiceCall() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        startActivity(Intent.createChooser(intent, "Call " + otherUserName + " with"));
    }

    private void startVideoCall() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://meet.google.com/new"));
        startActivity(Intent.createChooser(intent, "Video call with " + otherUserName));
    }

    private void loadMessages() {
        chatRef.addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    addMessageToChat(message);
                }
            }
            @Override public void onChildChanged(com.google.firebase.database.DataSnapshot snapshot, String s) {}
            @Override public void onChildRemoved(com.google.firebase.database.DataSnapshot snapshot) {}
            @Override public void onChildMoved(com.google.firebase.database.DataSnapshot snapshot, String s) {}
            @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void addMessageToChat(Message message) {
        boolean isMe = message.getUserId().equals(userId);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        if ("image".equals(message.getType()) && message.getFileUrl() != null) {
            ImageView imageView = new ImageView(this);
            Glide.with(this).load(message.getFileUrl()).into(imageView);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 8, 8, 8);
            imageView.setLayoutParams(params);
            imageView.setMaxHeight(1200);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatActivity.this, FullscreenImageActivity.class);
                intent.putExtra("image_url", message.getFileUrl());
                startActivity(intent);
            });
            container.addView(imageView);
        } else if ("file".equals(message.getType()) && message.getFileUrl() != null) {
            LinearLayout fileLayout = new LinearLayout(this);
            fileLayout.setOrientation(LinearLayout.HORIZONTAL);
            fileLayout.setPadding(20, 12, 20, 12);
            fileLayout.setBackgroundResource(isMe ? R.drawable.bg_user_message : R.drawable.bg_ai_message);
            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_file);
            icon.setLayoutParams(new LinearLayout.LayoutParams(32, 32));
            icon.setPadding(0, 0, 16, 0);
            TextView fileName = new TextView(this);
            fileName.setText(message.getText());
            fileName.setTextSize(14);
            fileName.setTextColor(isMe ? 0xFFFFFFFF : 0xFF000000);
            fileLayout.addView(icon);
            fileLayout.addView(fileName);
            fileLayout.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(message.getFileUrl()), "*/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Open with"));
            });
            container.addView(fileLayout);
        } else {
            TextView textTv = new TextView(this);
            textTv.setText(message.getText());
            textTv.setTextSize(16);
            textTv.setPadding(20, 12, 20, 12);
            textTv.setBackgroundResource(isMe ? R.drawable.bg_user_message : R.drawable.bg_ai_message);
            textTv.setTextColor(isMe ? 0xFFFFFFFF : 0xFF000000);
            container.addView(textTv);
        }

        TextView timeTv = new TextView(this);
        timeTv.setText(message.getTimestamp());
        timeTv.setTextSize(10);
        timeTv.setTextColor(0xFF666666);
        timeTv.setPadding(8, 2, 8, 2);
        container.addView(timeTv);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.setMargins(isMe ? 100 : 16, 8, isMe ? 16 : 100, 8);
        containerParams.gravity = isMe ? android.view.Gravity.END : android.view.Gravity.START;
        container.setLayoutParams(containerParams);

        llMessages.addView(container);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    public static class Message {
        public String userId;
        public String text;
        public String timestamp;
        public String type;
        public String fileUrl;
        public String mimeType;
        public String fileName;
        public Message() {}
        public Message(String userId, String text, String timestamp, String type,
                       String fileUrl, String mimeType, String fileName) {
            this.userId = userId;
            this.text = text;
            this.timestamp = timestamp;
            this.type = type;
            this.fileUrl = fileUrl;
            this.mimeType = mimeType;
            this.fileName = fileName;
        }
        public String getUserId() { return userId; }
        public String getText() { return text; }
        public String getTimestamp() { return timestamp; }
        public String getType() { return type; }
        public String getFileUrl() { return fileUrl; }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}