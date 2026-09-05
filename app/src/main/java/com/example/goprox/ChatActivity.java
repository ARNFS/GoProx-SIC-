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
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

import com.bumptech.glide.Glide;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends BaseActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int PICK_IMAGE = 100;
    private static final int PICK_FILE = 101;

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach, btnBack, btnMic, btnVideoCall;
    private TextView tvUserName;
    private CircleImageView ivProfilePhoto;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference chatRef;
    private ChildEventListener msgListener;
    private AudioRecorder audioRecorder;
    private String currentAudioFile;
    private volatile boolean isRecordingAudio = false;
    private Handler recordingHandler;

    private LinearLayout llRecordingOverlay;
    private TextView tvRecordingTime;
    private View viewVisualizer;
    private long recordingStartTime;

    private String chatId, otherUserId, currentUserId;
    private Uri fileUri;

    private final String FIREBASE_DB_URL =
            "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app";

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

        // 🔥 FIX: Մաքրում ենք messageList-ը նոր chat-ի համար
        messageList.clear();

        // 🔐 Յուրաքանչյուր user գրանցում է միայն ԻՐ UID-ն participants-ում
        ensureCurrentUserParticipant();

        initViews();

        if (otherUserName != null && !otherUserName.isEmpty()) {
            tvUserName.setText(otherUserName);
        } else {
            loadReceiverName();
        }

        loadProfilePhoto();
        setupFirebase();
        markMessagesAsRead();
    }

    // ================== CHAT PARTICIPANT ==================

    /**
     * Ստեղծում է միայն current user's participant entry-ն։
     *
     * Structure:
     *
     * chats/
     *   {chatId}/
     *     participants/
     *       {currentUserId}: true
     */
    private void ensureCurrentUserParticipant() {
        if (chatId == null || currentUserId == null) {
            return;
        }

        try {
            DatabaseReference participantRef =
                    FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                            .getReference("chats")
                            .child(chatId)
                            .child("participants")
                            .child(currentUserId);

            participantRef.setValue(true)
                    .addOnFailureListener(e -> {
                        // Chat-ը չենք կանգնեցնում participant write-ի failure-ի պատճառով։
                        // Security rules-ի հաջորդ փուլում դա կդառնա backend requirement։
                    });

        } catch (Exception ignored) {
        }
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
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        llRecordingOverlay = findViewById(R.id.llRecordingOverlay);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        viewVisualizer = findViewById(R.id.viewVisualizer);

        if (recyclerView == null || btnSend == null || etMessage == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ChatAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendTextMessage());

        btnBack.setOnClickListener(v -> finish());

        btnAttach.setOnClickListener(
                v -> showAttachmentDialog()
        );

        btnMic.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    checkAndStartVoiceRecording();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopVoiceRecordingAndSend();
                    return true;
            }

            return false;
        });

        if (btnVideoCall != null) {
            btnVideoCall.setOnClickListener(
                    v -> CallHelper.startCall(
                            ChatActivity.this,
                            otherUserId,
                            tvUserName != null
                                    ? tvUserName.getText().toString()
                                    : "User",
                            null
                    )
            );
        }
    }

    // ================== PROFILE PHOTO ==================

    private void loadProfilePhoto() {

        if (ivProfilePhoto == null || otherUserId == null) {
            return;
        }

        FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                .getReference("users")
                .child(otherUserId)
                .child("photoUrl")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                String photoUrl =
                                        snapshot.getValue(String.class);

                                if (photoUrl != null
                                        && !photoUrl.isEmpty()) {

                                    Glide.with(ChatActivity.this)
                                            .load(photoUrl)
                                            .placeholder(
                                                    R.drawable.ic_profile_placeholder
                                            )
                                            .error(
                                                    R.drawable.ic_profile_placeholder
                                            )
                                            .into(ivProfilePhoto);
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                            }
                        }
                );
    }

    // ================== VOICE RECORDING ==================

    private void checkAndStartVoiceRecording() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_RECORD_AUDIO_PERMISSION
            );

        } else {
            startVoiceRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startVoiceRecording();

            } else {

                Toast.makeText(
                        this,
                        "Microphone permission is required",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void startVoiceRecording() {

        if (audioRecorder == null) {
            audioRecorder = new AudioRecorder();
        }

        File cacheDir = getCacheDir();

        if (cacheDir == null) {
            Toast.makeText(
                    this,
                    "Cannot access storage",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        currentAudioFile =
                cacheDir.getAbsolutePath()
                        + "/voice_"
                        + System.currentTimeMillis()
                        + ".m4a";

        recordingStartTime =
                System.currentTimeMillis();

        try {

            audioRecorder.startRecording(
                    currentAudioFile,
                    amplitude ->
                            updateVisualizer(amplitude)
            );

            isRecordingAudio = true;

            showRecordingOverlay();

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "Failed to start recording",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateVisualizer(int amplitude) {

        if (viewVisualizer != null) {

            float scale =
                    Math.min(
                            amplitude / 32768f,
                            1.0f
                    );

            viewVisualizer.setScaleX(
                    0.5f + scale * 1.5f
            );

            viewVisualizer.setScaleY(
                    0.5f + scale * 1.5f
            );
        }
    }

    private void showRecordingOverlay() {

        if (llRecordingOverlay != null) {
            llRecordingOverlay.setVisibility(
                    View.VISIBLE
            );
        }

        startRecordingTimer();
    }

    private void hideRecordingOverlay() {

        if (llRecordingOverlay != null) {
            llRecordingOverlay.setVisibility(
                    View.GONE
            );
        }
    }

    private void startRecordingTimer() {

        final long[] startTime = {
                System.currentTimeMillis()
        };

        recordingHandler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (isRecordingAudio
                                && tvRecordingTime != null) {

                            long elapsed =
                                    System.currentTimeMillis()
                                            - startTime[0];

                            long seconds =
                                    elapsed / 1000;

                            long minutes =
                                    seconds / 60;

                            seconds =
                                    seconds % 60;

                            tvRecordingTime.setText(
                                    String.format(
                                            "%02d:%02d",
                                            minutes,
                                            seconds
                                    )
                            );

                            recordingHandler.postDelayed(
                                    this,
                                    1000
                            );
                        }
                    }
                },
                0
        );
    }

    private void stopVoiceRecordingAndSend() {

        if (audioRecorder != null
                && isRecordingAudio) {

            int duration =
                    (int) (
                            (
                                    System.currentTimeMillis()
                                            - recordingStartTime
                            ) / 1000
                    );

            audioRecorder.stopRecording();

            isRecordingAudio = false;

            hideRecordingOverlay();

            if (duration < 1) {

                Toast.makeText(
                        this,
                        "Too short",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            File audioFile =
                    new File(currentAudioFile);

            if (audioFile.exists()) {

                if (Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.N) {

                    fileUri =
                            FileProvider.getUriForFile(
                                    this,
                                    getPackageName()
                                            + ".fileprovider",
                                    audioFile
                            );

                } else {

                    fileUri =
                            Uri.fromFile(audioFile);
                }

                uploadVoiceMessageAndSend(
                        duration
                );
            }
        }
    }

    private void uploadVoiceMessageAndSend(
            int duration
    ) {

        if (fileUri == null) {
            return;
        }

        Toast.makeText(
                this,
                "Sending voice...",
                Toast.LENGTH_SHORT
        ).show();

        String fileName =
                System.currentTimeMillis()
                        + "_"
                        + (
                        fileUri.getLastPathSegment() != null
                                ? fileUri.getLastPathSegment()
                                : "voice.m4a"
                );

        StorageReference storageRef =
                FirebaseStorage.getInstance()
                        .getReference(
                                "chat_attachments"
                        )
                        .child(chatId)
                        .child(fileName);

        storageRef
                .putFile(fileUri)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {

                        throw task.getException() != null
                                ? task.getException()
                                : new Exception(
                                "Upload failed"
                        );
                    }

                    return storageRef.getDownloadUrl();

                })
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String downloadUrl =
                                task.getResult().toString();

                        sendVoiceMessage(
                                downloadUrl,
                                duration
                        );

                    } else {

                        Toast.makeText(
                                this,
                                "Upload failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void sendVoiceMessage(
            String fileUrl,
            int duration
    ) {

        if (chatRef == null
                || currentUserId == null) {

            return;
        }

        String msgId =
                chatRef.push().getKey();

        if (msgId == null) {
            return;
        }

        long timestamp =
                System.currentTimeMillis();

        ChatMessage msg =
                new ChatMessage(
                        msgId,
                        currentUserId,
                        "Voice message",
                        "voice",
                        fileUrl,
                        timestamp
                );

        msg.setVoiceDuration(duration);

        chatRef
                .child(msgId)
                .setValue(msg)
                .addOnSuccessListener(
                        aVoid ->
                                updateChatMeta(
                                        "Voice message",
                                        timestamp,
                                        "voice"
                                )
                );
    }

    // ================== FIREBASE ==================

    private void loadReceiverName() {

        try {

            FirebaseDatabase.getInstance(
                            FIREBASE_DB_URL
                    )
                    .getReference("users")
                    .child(otherUserId)
                    .child("name")
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {

                                @Override
                                public void onDataChange(
                                        @NonNull DataSnapshot snapshot
                                ) {

                                    String name =
                                            snapshot.getValue(
                                                    String.class
                                            );

                                    if (name != null
                                            && tvUserName != null) {

                                        tvUserName.setText(name);
                                    }
                                }

                                @Override
                                public void onCancelled(
                                        @NonNull DatabaseError error
                                ) {
                                }
                            }
                    );

        } catch (Exception ignored) {
        }
    }

    private void setupFirebase() {

        if (msgListener != null
                && chatRef != null) {

            chatRef.removeEventListener(
                    msgListener
            );
        }

        try {

            chatRef =
                    FirebaseDatabase
                            .getInstance(FIREBASE_DB_URL)
                            .getReference("chats")
                            .child(chatId)
                            .child("messages");

            Query query =
                    chatRef
                            .orderByKey()
                            .limitToLast(50);

            msgListener =
                    new ChildEventListener() {

                        @Override
                        public void onChildAdded(
                                @NonNull DataSnapshot snapshot,
                                String prev
                        ) {

                            ChatMessage msg =
                                    snapshot.getValue(
                                            ChatMessage.class
                                    );

                            if (msg != null
                                    && adapter != null) {

                                messageList.add(msg);

                                adapter.notifyItemInserted(
                                        messageList.size() - 1
                                );

                                if (recyclerView != null) {

                                    recyclerView.smoothScrollToPosition(
                                            messageList.size() - 1
                                    );
                                }
                            }
                        }

                        @Override
                        public void onChildChanged(
                                @NonNull DataSnapshot s,
                                String p
                        ) {
                        }

                        @Override
                        public void onChildRemoved(
                                @NonNull DataSnapshot s
                        ) {
                        }

                        @Override
                        public void onChildMoved(
                                @NonNull DataSnapshot s,
                                String p
                        ) {
                        }

                        @Override
                        public void onCancelled(
                                @NonNull DatabaseError error
                        ) {

                            Toast.makeText(
                                    ChatActivity.this,
                                    "Failed to load messages",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    };

            query.addChildEventListener(
                    msgListener
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Chat initialization error",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void sendTextMessage() {

        if (etMessage == null) {
            return;
        }

        String text =
                etMessage
                        .getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {
            return;
        }

        sendMessage(
                "text",
                text,
                null
        );
    }

    private void sendMessage(
            String type,
            String content,
            String fileUrl
    ) {

        if (chatRef == null
                || currentUserId == null) {

            return;
        }

        String msgId =
                chatRef.push().getKey();

        if (msgId == null) {
            return;
        }

        long timestamp =
                System.currentTimeMillis();

        ChatMessage msg =
                new ChatMessage(
                        msgId,
                        currentUserId,
                        content,
                        type,
                        fileUrl,
                        timestamp
                );

        chatRef
                .child(msgId)
                .setValue(msg)
                .addOnSuccessListener(
                        aVoid -> {

                            if (etMessage != null) {
                                etMessage.setText("");
                            }

                            updateChatMeta(
                                    content,
                                    timestamp,
                                    type
                            );
                        }
                );
    }

    private void updateChatMeta(
            String lastMsg,
            long timestamp,
            String type
    ) {

        try {

            DatabaseReference userChatsRef =
                    FirebaseDatabase
                            .getInstance(FIREBASE_DB_URL)
                            .getReference("user_chats");

            Map<String, Object> updates =
                    new HashMap<>();

            updates.put(
                    currentUserId
                            + "/"
                            + otherUserId
                            + "/lastMessage",
                    lastMsg
            );

            updates.put(
                    currentUserId
                            + "/"
                            + otherUserId
                            + "/timestamp",
                    timestamp
            );

            updates.put(
                    currentUserId
                            + "/"
                            + otherUserId
                            + "/chatId",
                    chatId
            );

            updates.put(
                    currentUserId
                            + "/"
                            + otherUserId
                            + "/lastMessageType",
                    type
            );

            Map<String, Object> otherUpdates =
                    new HashMap<>();

            otherUpdates.put(
                    "lastMessage",
                    lastMsg
            );

            otherUpdates.put(
                    "timestamp",
                    timestamp
            );

            otherUpdates.put(
                    "chatId",
                    chatId
            );

            otherUpdates.put(
                    "lastMessageType",
                    type
            );

            otherUpdates.put(
                    "unreadCount",
                    ServerValue.increment(1)
            );

            updates.put(
                    otherUserId
                            + "/"
                            + currentUserId,
                    otherUpdates
            );

            userChatsRef.updateChildren(
                    updates
            );

        } catch (Exception ignored) {
        }
    }

    // 🔥 MARK MESSAGES AS READ — update Firebase + local

    private void markMessagesAsRead() {

        try {

            FirebaseDatabase
                    .getInstance(FIREBASE_DB_URL)
                    .getReference("user_chats")
                    .child(currentUserId)
                    .child(otherUserId)
                    .child("unreadCount")
                    .setValue(0);

            for (ChatMessage msg :
                    messageList) {

                if (msg.getSenderId() != null
                        && !msg.getSenderId()
                        .equals(currentUserId)
                        && !msg.isRead()) {

                    msg.setRead(true);

                    if (chatRef != null) {

                        chatRef
                                .child(msg.getId())
                                .child("isRead")
                                .setValue(true);
                    }
                }
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

        } catch (Exception ignored) {
        }
    }

    // ================== ATTACHMENTS ==================

    private void showAttachmentDialog() {

        try {

            String[] options = {
                    "Image",
                    "File"
            };

            new AlertDialog.Builder(this)
                    .setTitle("Attach")
                    .setItems(
                            options,
                            (dialog, which) -> {

                                if (which == 0) {

                                    Intent intent =
                                            new Intent(
                                                    Intent.ACTION_PICK
                                            );

                                    intent.setType(
                                            "image/*"
                                    );

                                    startActivityForResult(
                                            intent,
                                            PICK_IMAGE
                                    );

                                } else {

                                    Intent intent =
                                            new Intent(
                                                    Intent.ACTION_GET_CONTENT
                                            );

                                    intent.setType("*/*");

                                    startActivityForResult(
                                            intent,
                                            PICK_FILE
                                    );
                                }
                            }
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Attachment error",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            fileUri = data.getData();

            uploadFileAndSend("file");
        }
    }

    private void uploadFileAndSend(
            String messageType
    ) {

        if (fileUri == null) {
            return;
        }

        Toast.makeText(
                this,
                "Uploading...",
                Toast.LENGTH_SHORT
        ).show();

        String fileName =
                System.currentTimeMillis()
                        + "_"
                        + (
                        fileUri.getLastPathSegment() != null
                                ? fileUri.getLastPathSegment()
                                : "file"
                );

        StorageReference storageRef =
                FirebaseStorage
                        .getInstance()
                        .getReference("chat_attachments")
                        .child(chatId)
                        .child(fileName);

        storageRef
                .putFile(fileUri)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {

                        throw task.getException() != null
                                ? task.getException()
                                : new Exception(
                                "Upload failed"
                        );
                    }

                    return storageRef.getDownloadUrl();

                })
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String downloadUrl =
                                task.getResult().toString();

                        String displayName =
                                fileUri.getLastPathSegment();

                        String finalType =
                                messageType;

                        if ("file".equals(messageType)) {

                            finalType =
                                    fileUri.toString()
                                            .contains("image")
                                            ? "image"
                                            : "file";
                        }

                        sendMessage(
                                finalType,
                                displayName,
                                downloadUrl
                        );

                        Toast.makeText(
                                this,
                                "Upload successful",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Exception e =
                                task.getException();

                        Toast.makeText(
                                this,
                                "Upload failed: "
                                        + (
                                        e != null
                                                ? e.getMessage()
                                                : "Unknown error"
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    public void openFile(
            String fileUrl
    ) {

        if (fileUrl == null
                || fileUrl.isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid file URL",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW
                );

        intent.setDataAndType(
                Uri.parse(fileUrl),
                getMimeTypeFromUrl(fileUrl)
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        if (intent.resolveActivity(
                getPackageManager()
        ) != null) {

            try {

                startActivity(
                        Intent.createChooser(
                                intent,
                                "Open with"
                        )
                );

                return;

            } catch (ActivityNotFoundException ignored) {
            }
        }

        Toast.makeText(
                this,
                "Downloading file...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {

            try {

                URL url =
                        new URL(fileUrl);

                HttpURLConnection connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setConnectTimeout(
                        10000
                );

                connection.setReadTimeout(
                        10000
                );

                connection.connect();

                String fName =
                        Uri.parse(fileUrl)
                                .getLastPathSegment();

                if (fName == null
                        || !fName.contains(".")) {

                    fName =
                            "file_"
                                    + System.currentTimeMillis();
                }

                File tempFile =
                        new File(
                                getCacheDir(),
                                fName
                        );

                try (
                        InputStream input =
                                connection
                                        .getInputStream();

                        FileOutputStream out =
                                new FileOutputStream(
                                        tempFile
                                )
                ) {

                    byte[] buffer =
                            new byte[4096];

                    int bytesRead;

                    while (
                            (bytesRead =
                                    input.read(buffer))
                                    != -1
                    ) {

                        out.write(
                                buffer,
                                0,
                                bytesRead
                        );
                    }
                }

                connection.disconnect();

                runOnUiThread(
                        () ->
                                openLocalFile(
                                        tempFile
                                )
                );

            } catch (Exception e) {

                runOnUiThread(
                        () ->
                                Toast.makeText(
                                        this,
                                        "Download failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                );
            }

        }).start();
    }

    private String getMimeTypeFromUrl(
            String url
    ) {

        try {

            String ext =
                    MimeTypeMap
                            .getFileExtensionFromUrl(
                                    url
                            );

            if (ext != null) {

                return MimeTypeMap
                        .getSingleton()
                        .getMimeTypeFromExtension(
                                ext.toLowerCase()
                        );
            }

        } catch (Exception ignored) {
        }

        return "*/*";
    }

    private void openLocalFile(
            File file
    ) {

        if (file == null
                || !file.exists()) {

            Toast.makeText(
                    this,
                    "File not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            Uri uri;

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.N) {

                uri =
                        FileProvider.getUriForFile(
                                this,
                                getPackageName()
                                        + ".fileprovider",
                                file
                        );

            } else {

                uri =
                        Uri.fromFile(file);
            }

            String mimeType =
                    getMimeType(file);

            if (mimeType == null) {
                mimeType = "*/*";
            }

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            intent.setDataAndType(
                    uri,
                    mimeType
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Open with"
                    )
            );

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "No app found to open this file",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error opening file",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String getMimeType(
            File file
    ) {

        try {

            String ext =
                    MimeTypeMap
                            .getFileExtensionFromUrl(
                                    file.getAbsolutePath()
                            );

            if (ext != null) {

                return MimeTypeMap
                        .getSingleton()
                        .getMimeTypeFromExtension(
                                ext.toLowerCase()
                        );
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (msgListener != null
                && chatRef != null) {

            try {

                chatRef.removeEventListener(
                        msgListener
                );

            } catch (Exception ignored) {
            }
        }

        if (adapter != null) {

            try {
                adapter.stopPlaying();
            } catch (Exception ignored) {
            }
        }

        if (audioRecorder != null
                && isRecordingAudio) {

            try {
                audioRecorder.stopRecording();
            } catch (Exception ignored) {
            }
        }

        if (recordingHandler != null) {

            recordingHandler
                    .removeCallbacksAndMessages(
                            null
                    );
        }
    }
}
