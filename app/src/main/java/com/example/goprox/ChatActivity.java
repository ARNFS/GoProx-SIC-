package com.example.goprox;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int PICK_IMAGE = 100;
    private static final int PICK_FILE = 101;

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach, btnBack, btnMic, btnVideoCall;
    private TextView tvUserName;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference chatRef;
    private ChildEventListener msgListener;
    private AudioRecorder audioRecorder;
    private String currentAudioFile;
    private volatile boolean isRecordingAudio = false;
    private AlertDialog recordingDialog;
    private TextView tvRecordingTime;
    private Handler recordingHandler;

    private String chatId, otherUserId, currentUserId;
    private Uri fileUri;

    private final String FIREBASE_DB_URL =
            "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra("otherUserId");
        String otherUserName = getIntent().getStringExtra("otherUserName");

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: user not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();
        chatId = (currentUserId.compareTo(otherUserId) < 0)
                ? currentUserId + "_" + otherUserId
                : otherUserId + "_" + currentUserId;

        recordingHandler = new Handler(Looper.getMainLooper());

        initViews();
        if (otherUserName != null && !otherUserName.isEmpty()) {
            tvUserName.setText(otherUserName);
        } else {
            loadReceiverName();
        }
        setupFirebase();
        markMessagesAsRead();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        btnBack = findViewById(R.id.btnBack);
        btnMic = findViewById(R.id.btnMic);
        btnVideoCall = findViewById(R.id.btnVideoCall);
        tvUserName = findViewById(R.id.tvUserName);

        if (recyclerView == null || btnSend == null || etMessage == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> showAttachmentDialog());
        btnMic.setOnClickListener(v -> checkAndStartVoiceRecording());
        if (btnVideoCall != null) {
            btnVideoCall.setOnClickListener(v -> {
                // 🔥 Օգտագործում ենք CallHelper-ը, serviceTitle-ը դատարկ (chat-ից զանգի դեպքում)
                CallHelper.startCall(ChatActivity.this, otherUserId,
                        tvUserName != null ? tvUserName.getText().toString() : "User",
                        null);
            });
        }
    }

    // ================== VOICE RECORDING ==================
    private void checkAndStartVoiceRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startVoiceRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                Toast.makeText(this,
                        "Microphone permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startVoiceRecording() {
        if (audioRecorder == null) audioRecorder = new AudioRecorder();

        File cacheDir = getCacheDir();
        if (cacheDir == null) {
            Toast.makeText(this, "Cannot access storage", Toast.LENGTH_SHORT).show();
            return;
        }
        currentAudioFile = cacheDir.getAbsolutePath()
                + "/voice_" + System.currentTimeMillis() + ".m4a";

        try {
            audioRecorder.startRecording(currentAudioFile, amplitude -> {});
            isRecordingAudio = true;
            showRecordingDialog();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRecordingDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_recording, null);
            if (view == null) return;

            tvRecordingTime = view.findViewById(R.id.tvRecordingTime);
            Button btnStop = view.findViewById(R.id.btnStopRecording);

            recordingDialog = builder.setView(view).setCancelable(false).create();
            recordingDialog.show();

            btnStop.setOnClickListener(v -> stopVoiceRecording());
            startRecordingTimer();
        } catch (Exception e) {
            Toast.makeText(this, "Recording dialog error", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecordingTimer() {
        final long[] startTime = {System.currentTimeMillis()};
        recordingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecordingAudio && tvRecordingTime != null) {
                    long elapsed = System.currentTimeMillis() - startTime[0];
                    long seconds = elapsed / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    tvRecordingTime.setText(String.format("%02d:%02d", minutes, seconds));
                    recordingHandler.postDelayed(this, 1000);
                }
            }
        }, 0);
    }

    private void stopVoiceRecording() {
        if (audioRecorder != null && isRecordingAudio) {
            audioRecorder.stopRecording();
            isRecordingAudio = false;
            if (recordingDialog != null && recordingDialog.isShowing()) {
                try {
                    recordingDialog.dismiss();
                } catch (Exception ignored) {}
            }
            File audioFile = new File(currentAudioFile);
            if (audioFile.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", audioFile);
                } else {
                    fileUri = Uri.fromFile(audioFile);
                }
                uploadFileAndSend("voice");
            }
        }
    }

    // ================== FIREBASE ==================
    private void loadReceiverName() {
        try {
            FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                    .getReference("users").child(otherUserId).child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String name = snapshot.getValue(String.class);
                            if (name != null && tvUserName != null) {
                                tvUserName.setText(name);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        } catch (Exception ignored) {}
    }

    private void setupFirebase() {
        try {
            chatRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                    .getReference("chats").child(chatId).child("messages");
            Query query = chatRef.orderByKey().limitToLast(50);

            msgListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                    ChatMessage msg = snapshot.getValue(ChatMessage.class);
                    if (msg != null && adapter != null) {
                        messageList.add(msg);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        if (recyclerView != null) {
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot s, String p) {}

                @Override
                public void onChildRemoved(@NonNull DataSnapshot s) {}

                @Override
                public void onChildMoved(@NonNull DataSnapshot s, String p) {}

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatActivity.this,
                            "Failed to load messages", Toast.LENGTH_SHORT).show();
                }
            };
            query.addChildEventListener(msgListener);
        } catch (Exception e) {
            Toast.makeText(this, "Chat initialization error", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendTextMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        sendMessage("text", text, null);
    }

    private void sendMessage(String type, String content, String fileUrl) {
        if (chatRef == null || currentUserId == null) return;

        String msgId = chatRef.push().getKey();
        if (msgId == null) return;

        long timestamp = System.currentTimeMillis();
        ChatMessage msg = new ChatMessage(msgId, currentUserId,
                content, type, fileUrl, timestamp);

        chatRef.child(msgId).setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    if (etMessage != null) etMessage.setText("");
                    updateChatMeta(content, timestamp, type);
                });
    }

    private void updateChatMeta(String lastMsg, long timestamp, String type) {
        try {
            DatabaseReference userChatsRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                    .getReference("user_chats");
            Map<String, Object> updates = new HashMap<>();

            updates.put(currentUserId + "/" + otherUserId + "/lastMessage", lastMsg);
            updates.put(currentUserId + "/" + otherUserId + "/timestamp", timestamp);
            updates.put(currentUserId + "/" + otherUserId + "/chatId", chatId);
            updates.put(currentUserId + "/" + otherUserId + "/lastMessageType", type);

            Map<String, Object> otherUpdates = new HashMap<>();
            otherUpdates.put("lastMessage", lastMsg);
            otherUpdates.put("timestamp", timestamp);
            otherUpdates.put("chatId", chatId);
            otherUpdates.put("lastMessageType", type);
            otherUpdates.put("unreadCount", ServerValue.increment(1));
            updates.put(otherUserId + "/" + currentUserId, otherUpdates);

            userChatsRef.updateChildren(updates);
        } catch (Exception ignored) {}
    }

    private void markMessagesAsRead() {
        try {
            FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                    .getReference("user_chats")
                    .child(currentUserId)
                    .child(otherUserId)
                    .child("unreadCount")
                    .setValue(0);
        } catch (Exception ignored) {}
    }

    // ================== ATTACHMENTS ==================
    private void showAttachmentDialog() {
        try {
            String[] options = {"Image", "File"};
            new AlertDialog.Builder(this)
                    .setTitle("Attach")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, PICK_IMAGE);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");
                            startActivityForResult(intent, PICK_FILE);
                        }
                    }).show();
        } catch (Exception e) {
            Toast.makeText(this, "Attachment error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            uploadFileAndSend("file");
        }
    }

    private void uploadFileAndSend(String messageType) {
        if (fileUri == null) return;
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        String fileName = System.currentTimeMillis() + "_"
                + (fileUri.getLastPathSegment() != null
                ? fileUri.getLastPathSegment() : "file");

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("chat_attachments")
                .child(chatId)
                .child(fileName);

        storageRef.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Upload failed");
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().toString();
                        String displayName = fileUri.getLastPathSegment();
                        String finalType = messageType;
                        if ("file".equals(messageType)) {
                            finalType = (fileUri.toString().contains("image"))
                                    ? "image" : "file";
                        }
                        sendMessage(finalType, displayName, downloadUrl);
                        Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this,
                                "Upload failed: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void openFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            Toast.makeText(this, "Invalid file URL", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(fileUrl), getMimeTypeFromUrl(fileUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(Intent.createChooser(intent, "Open with"));
                return;
            } catch (ActivityNotFoundException ignored) {}
        }

        Toast.makeText(this, "Downloading file...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                String fName = Uri.parse(fileUrl).getLastPathSegment();
                if (fName == null || !fName.contains("."))
                    fName = "file_" + System.currentTimeMillis();

                File tempFile = new File(getCacheDir(), fName);
                try (InputStream input = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1)
                        out.write(buffer, 0, bytesRead);
                }
                connection.disconnect();
                runOnUiThread(() -> openLocalFile(tempFile));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Download failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String getMimeTypeFromUrl(String url) {
        try {
            String ext = MimeTypeMap.getFileExtensionFromUrl(url);
            if (ext != null)
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
        } catch (Exception ignored) {}
        return "*/*";
    }

    private void openLocalFile(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", file);
            } else {
                uri = Uri.fromFile(file);
            }
            String mimeType = getMimeType(file);
            if (mimeType == null) mimeType = "*/*";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(File file) {
        try {
            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
            if (ext != null)
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (msgListener != null && chatRef != null) {
            try {
                chatRef.removeEventListener(msgListener);
            } catch (Exception ignored) {}
        }
        if (adapter != null) {
            try {
                adapter.stopPlaying();
            } catch (Exception ignored) {}
        }
        if (audioRecorder != null && isRecordingAudio) {
            try {
                audioRecorder.stopRecording();
            } catch (Exception ignored) {}
        }
        if (recordingDialog != null && recordingDialog.isShowing()) {
            try {
                recordingDialog.dismiss();
            } catch (Exception ignored) {}
        }
        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }
    }
}