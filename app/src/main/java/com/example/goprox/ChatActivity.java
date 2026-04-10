package com.example.goprox;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import androidx.appcompat.app.AppCompatActivity;
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

public class ChatActivity extends BaseActivity{

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach, btnBack, btnMic;
    private TextView tvUserName;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference chatRef;
    private ChildEventListener msgListener;
    private AudioRecorder audioRecorder;
    private String currentAudioFile;
    private boolean isRecordingAudio = false;
    private AlertDialog recordingDialog;
    private TextView tvRecordingTime;

    private String chatId, otherUserId, currentUserId;
    private static final int PICK_IMAGE = 100;
    private static final int PICK_FILE = 101;
    private Uri fileUri;

    private final String FIREBASE_DB_URL = "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app/";

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

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatId = (currentUserId.compareTo(otherUserId) < 0) ?
                currentUserId + "_" + otherUserId : otherUserId + "_" + currentUserId;

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
        tvUserName = findViewById(R.id.tvUserName);

        adapter = new ChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> showAttachmentDialog());
        btnMic.setOnClickListener(v -> checkAndStartVoiceRecording());
    }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show();
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
        currentAudioFile = cacheDir.getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".m4a";

        try {
            audioRecorder.startRecording(currentAudioFile, amplitude -> {});
            isRecordingAudio = true;
            showRecordingDialog();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_recording, null);
        tvRecordingTime = view.findViewById(R.id.tvRecordingTime);
        Button btnStop = view.findViewById(R.id.btnStopRecording);

        recordingDialog = builder.setView(view).setCancelable(false).create();
        recordingDialog.show();

        btnStop.setOnClickListener(v -> stopVoiceRecording());
        startRecordingTimer();
    }

    private void startRecordingTimer() {
        final long[] startTime = {System.currentTimeMillis()};
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecordingAudio && tvRecordingTime != null) {
                    long elapsed = System.currentTimeMillis() - startTime[0];
                    long seconds = elapsed / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    tvRecordingTime.setText(String.format("%02d:%02d", minutes, seconds));
                    handler.postDelayed(this, 1000);
                }
            }
        }, 0);
    }

    private void stopVoiceRecording() {
        if (audioRecorder != null && isRecordingAudio) {
            audioRecorder.stopRecording();
            isRecordingAudio = false;

            if (recordingDialog != null) recordingDialog.dismiss();

            fileUri = Uri.fromFile(new File(currentAudioFile));
            uploadFileAndSend("voice");
        }
    }

    private void loadReceiverName() {
        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("users").child(otherUserId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null) tvUserName.setText(name);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupFirebase() {
        chatRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("chats").child(chatId).child("messages");
        Query query = chatRef.orderByKey().limitToLast(50);

        msgListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    messageList.add(msg);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };
        query.addChildEventListener(msgListener);
    }

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        sendMessage("text", text, null);
    }

    private void sendMessage(String type, String content, String fileUrl) {
        String msgId = chatRef.push().getKey();
        long timestamp = System.currentTimeMillis();
        ChatMessage msg = new ChatMessage(msgId, currentUserId, content, type, fileUrl, timestamp);

        chatRef.child(msgId).setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                    updateChatMeta(content, timestamp, type);
                });
    }

    private void updateChatMeta(String lastMsg, long timestamp, String type) {
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
    }

    private void markMessagesAsRead() {
        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("user_chats")
                .child(currentUserId)
                .child(otherUserId)
                .child("unreadCount")
                .setValue(0);
    }

    private void showAttachmentDialog() {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            uploadFileAndSend("file");
        }
    }

    private void uploadFileAndSend(String messageType) {
        if (fileUri == null) return;

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        String fileName = System.currentTimeMillis() + "_" +
                (fileUri.getLastPathSegment() != null ? fileUri.getLastPathSegment() : "file");

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("chat_attachments")
                .child(chatId)
                .child(fileName);

        storageRef.putFile(fileUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if (progress % 20 == 0) {
                        Toast.makeText(this, "Uploading: " + (int)progress + "%", Toast.LENGTH_SHORT).show();
                    }
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().toString();
                        String displayName = fileUri.getLastPathSegment();
                        String finalType = messageType;
                        if ("file".equals(messageType)) {
                            finalType = fileUri.toString().contains("image") ? "image" : "file";
                        }
                        sendMessage(finalType, displayName, downloadUrl);
                        Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --------------------------------------------------------------
    // НОВЫЙ МЕТОД: скачать и открыть файл по HTTPS-ссылке
    // --------------------------------------------------------------
    public void openFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            Toast.makeText(this, "Invalid file URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сначала пробуем открыть напрямую по URL
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(fileUrl), getMimeTypeFromUrl(fileUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Проверяем, есть ли приложение, которое может обработать такой Intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(Intent.createChooser(intent, "Open with"));
                return; // Успешно открыли, выходим
            } catch (ActivityNotFoundException ignored) {
                // Если не получилось – идём дальше к скачиванию
            }
        }

        // Если напрямую не получилось – скачиваем и открываем локально
        Toast.makeText(this, "Downloading file...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                String fileName = Uri.parse(fileUrl).getLastPathSegment();
                if (fileName == null || !fileName.contains(".")) {
                    fileName = "file_" + System.currentTimeMillis();
                }

                File tempFile = new File(getCacheDir(), fileName);
                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }
                connection.disconnect();

                runOnUiThread(() -> openLocalFile(tempFile));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Вспомогательный метод: определяет MIME по расширению в URL
    private String getMimeTypeFromUrl(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return "*/*";
    }
    private void openLocalFile(File file) {
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }

        String mimeType = getMimeType(file);
        if (mimeType == null) mimeType = "*/*";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return null;
    }
    // --------------------------------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (msgListener != null) chatRef.removeEventListener(msgListener);
        if (adapter != null) adapter.stopPlaying();
    }
}