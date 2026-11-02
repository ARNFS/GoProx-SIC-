
package com.example.goprox;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int PICK_IMAGE = 100;
    private static final int PICK_FILE = 101;

    private static final long MAX_CHAT_ATTACHMENT_BYTES =
            25L * 1024L * 1024L;

    private static final String FIREBASE_DB_URL =
            "https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app";

    private static final String[] CHAT_ATTACHMENT_MIME_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint"
    };

    private RecyclerView recyclerView;

    private EditText etMessage;

    private ImageButton btnSend;
    private ImageButton btnAttach;
    private ImageButton btnBack;
    private ImageButton btnMic;
    private ImageButton btnVideoCall;

    private TextView tvUserName;
    private CircleImageView ivProfilePhoto;

    private LinearLayout llRecordingOverlay;
    private TextView tvRecordingTime;
    private View viewVisualizer;

    private ChatAdapter adapter;

    private final List<ChatMessage> messageList =
            new ArrayList<>();

    private DatabaseReference chatRef;

    private Query messagesQuery;

    private ChildEventListener msgListener;

    private AudioRecorder audioRecorder;

    private String currentAudioFile;

    private volatile boolean isRecordingAudio = false;

    private Handler recordingHandler;

    private long recordingStartTime;

    private String chatId;
    private String otherUserId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        otherUserId =
                getIntent().getStringExtra("otherUserId");

        String otherUserName =
                getIntent().getStringExtra("otherUserName");

        if (otherUserId == null
                || otherUserId.trim().isEmpty()) {

            showErrorAndFinish(
                    "Error: user not specified"
            );

            return;
        }

        otherUserId = otherUserId.trim();

        FirebaseAuth auth =
                FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {

            showErrorAndFinish(
                    "Please sign in"
            );

            return;
        }

        currentUserId =
                auth.getCurrentUser().getUid();

        if (currentUserId.equals(otherUserId)) {

            showErrorAndFinish(
                    "You cannot start a chat with yourself"
            );

            return;
        }

        chatId =
                currentUserId.compareTo(otherUserId) < 0
                        ? currentUserId + "_" + otherUserId
                        : otherUserId + "_" + currentUserId;

        recordingHandler =
                new Handler(Looper.getMainLooper());

        messageList.clear();

        if (!initViews()) {
            return;
        }

        if (otherUserName != null
                && !otherUserName.trim().isEmpty()) {

            tvUserName.setText(
                    otherUserName.trim()
            );

        } else {

            loadReceiverName();
        }

        loadProfilePhoto();

        ensureCurrentUserParticipant();
    }

    // =========================================================
    // GENERAL HELPERS
    // =========================================================

    private void showErrorAndFinish(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        finish();
    }

    private boolean isActivityAvailable() {

        return !isFinishing()
                && !isDestroyed();
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================

    private boolean initViews() {

        recyclerView =
                findViewById(R.id.recyclerViewChat);

        etMessage =
                findViewById(R.id.etMessage);

        btnSend =
                findViewById(R.id.btnSend);

        btnAttach =
                findViewById(R.id.btnAttach);

        btnBack =
                findViewById(R.id.btnBack);

        btnMic =
                findViewById(R.id.btnMic);

        btnVideoCall =
                findViewById(R.id.btnVideoCall);

        tvUserName =
                findViewById(R.id.tvUserName);

        ivProfilePhoto =
                findViewById(R.id.ivProfilePhoto);

        llRecordingOverlay =
                findViewById(R.id.llRecordingOverlay);

        tvRecordingTime =
                findViewById(R.id.tvRecordingTime);

        viewVisualizer =
                findViewById(R.id.viewVisualizer);

        if (recyclerView == null
                || etMessage == null
                || btnSend == null
                || btnBack == null
                || tvUserName == null) {

            showErrorAndFinish(
                    "Chat UI initialization error"
            );

            return false;
        }

        adapter =
                new ChatAdapter(
                        messageList,
                        currentUserId
                );

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(
                layoutManager
        );

        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(
                v -> sendTextMessage()
        );

        btnBack.setOnClickListener(
                v -> finish()
        );

        if (btnAttach != null) {

            btnAttach.setOnClickListener(
                    v -> showAttachmentDialog()
            );
        }

        if (btnMic != null) {

            btnMic.setOnTouchListener(
                    (v, event) -> {

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                checkAndStartVoiceRecording();

                                return true;

                            case MotionEvent.ACTION_UP:

                                stopVoiceRecordingAndSend(true);

                                return true;

                            case MotionEvent.ACTION_CANCEL:

                                stopVoiceRecordingAndSend(false);

                                return true;

                            default:

                                return false;
                        }
                    }
            );
        }

        if (btnVideoCall != null) {

            btnVideoCall.setOnClickListener(
                    v -> CallHelper.startCall(
                            ChatActivity.this,
                            otherUserId,
                            tvUserName.getText().toString(),
                            null
                    )
            );
        }

        return true;
    }

    // =========================================================
    // CHAT PARTICIPANT INITIALIZATION
    // =========================================================

    private void ensureCurrentUserParticipant() {

        if (chatId == null
                || currentUserId == null
                || otherUserId == null) {

            showErrorAndFinish(
                    "Unable to initialize chat"
            );

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "otherUserId",
                otherUserId
        );

        try {

            FirebaseFunctions functions =
                    FirebaseFunctions.getInstance(
                            "europe-west1"
                    );

            functions
                    .getHttpsCallable(
                            "ensureChatParticipant"
                    )
                    .call(data)
                    .addOnSuccessListener(result -> {

                        if (!isActivityAvailable()) {
                            return;
                        }

                        Log.d(
                                TAG,
                                "ensureChatParticipant succeeded"
                        );

                        verifyParticipantCreated();
                    })
                    .addOnFailureListener(error -> {

                        Log.e(
                                TAG,
                                "ensureChatParticipant failed",
                                error
                        );

                        if (!isActivityAvailable()) {
                            return;
                        }

                        String message =
                                error.getMessage() != null
                                        ? error.getMessage()
                                        : "Unknown error";

                        showErrorAndFinish(
                                "Chat initialization failed: "
                                        + message
                        );
                    });

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Cloud Function exception",
                    e
            );

            showErrorAndFinish(
                    "Chat initialization error"
            );
        }
    }

    private void verifyParticipantCreated() {

        DatabaseReference participantRef =
                FirebaseDatabase
                        .getInstance(FIREBASE_DB_URL)
                        .getReference("chats")
                        .child(chatId)
                        .child("participants")
                        .child(currentUserId);

        participantRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!isActivityAvailable()) {
                            return;
                        }

                        Boolean participant =
                                snapshot.getValue(Boolean.class);

                        if (Boolean.TRUE.equals(participant)) {

                            setupFirebase();

                        } else {

                            showErrorAndFinish(
                                    "Chat participant initialization failed"
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "Participant verification failed",
                                error.toException()
                        );

                        if (isActivityAvailable()) {

                            showErrorAndFinish(
                                    "Unable to verify chat access"
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // PROFILE
    // =========================================================

    private void loadReceiverName() {

        if (otherUserId == null) {
            return;
        }

        FirebaseDatabase
                .getInstance(FIREBASE_DB_URL)
                .getReference("users")
                .child(otherUserId)
                .child("name")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!isActivityAvailable()) {
                                    return;
                                }

                                String name =
                                        snapshot.getValue(String.class);

                                if (name != null
                                        && !name.trim().isEmpty()) {

                                    tvUserName.setText(
                                            name.trim()
                                    );
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Log.w(
                                        TAG,
                                        "Could not load receiver name",
                                        error.toException()
                                );
                            }
                        }
                );
    }

    private void loadProfilePhoto() {

        if (ivProfilePhoto == null
                || otherUserId == null) {

            return;
        }

        FirebaseDatabase
                .getInstance(FIREBASE_DB_URL)
                .getReference("users")
                .child(otherUserId)
                .child("photoUrl")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!isActivityAvailable()) {
                                    return;
                                }

                                String photoUrl =
                                        snapshot.getValue(String.class);

                                if (photoUrl == null
                                        || photoUrl.trim().isEmpty()) {

                                    return;
                                }

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

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Log.w(
                                        TAG,
                                        "Profile photo loading cancelled",
                                        error.toException()
                                );
                            }
                        }
                );
    }

    // =========================================================
    // VOICE RECORDING
    // =========================================================

    private void checkAndStartVoiceRecording() {

        if (isRecordingAudio) {
            return;
        }

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

            return;
        }

        startVoiceRecording();
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

        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return;
        }

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    this,
                    "Microphone permission granted. Hold the microphone button to record.",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Microphone permission is required",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void startVoiceRecording() {

        if (isRecordingAudio || !isActivityAvailable()) {
            return;
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
                new File(
                        cacheDir,
                        "voice_" + UUID.randomUUID() + ".m4a"
                ).getAbsolutePath();

        recordingStartTime =
                System.currentTimeMillis();

        try {

            audioRecorder = new AudioRecorder();

            audioRecorder.startRecording(
                    currentAudioFile,
                    this::updateVisualizer
            );

            isRecordingAudio = true;

            if (tvRecordingTime != null) {
                tvRecordingTime.setText("00:00");
            }

            showRecordingOverlay();

        } catch (IOException | RuntimeException e) {

            Log.e(
                    TAG,
                    "Voice recording failed to start",
                    e
            );

            isRecordingAudio = false;

            audioRecorder = null;

            currentAudioFile = null;

            Toast.makeText(
                    this,
                    "Failed to start recording",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateVisualizer(int amplitude) {

        if (viewVisualizer == null) {
            return;
        }

        float scale =
                Math.min(
                        Math.max(amplitude / 32768f, 0f),
                        1f
                );

        runOnUiThread(() -> {

            if (!isRecordingAudio
                    || viewVisualizer == null
                    || !isActivityAvailable()) {

                return;
            }

            viewVisualizer.setScaleX(
                    0.5f + scale * 1.5f
            );

            viewVisualizer.setScaleY(
                    0.5f + scale * 1.5f
            );
        });
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

        if (viewVisualizer != null) {

            viewVisualizer.setScaleX(1f);
            viewVisualizer.setScaleY(1f);
        }
    }

    private void startRecordingTimer() {

        if (recordingHandler == null) {
            return;
        }

        recordingHandler.removeCallbacksAndMessages(null);

        Runnable timer = new Runnable() {

            @Override
            public void run() {

                if (!isRecordingAudio
                        || tvRecordingTime == null
                        || !isActivityAvailable()) {

                    return;
                }

                long elapsed =
                        System.currentTimeMillis()
                                - recordingStartTime;

                long totalSeconds =
                        elapsed / 1000L;

                long minutes =
                        totalSeconds / 60L;

                long seconds =
                        totalSeconds % 60L;

                tvRecordingTime.setText(
                        String.format(
                                Locale.US,
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
        };

        recordingHandler.post(timer);
    }

    private void stopVoiceRecordingAndSend(boolean shouldSend) {

        if (!isRecordingAudio) {
            return;
        }

        isRecordingAudio = false;

        if (recordingHandler != null) {

            recordingHandler.removeCallbacksAndMessages(null);
        }

        int duration =
                (int) (
                        (System.currentTimeMillis()
                                - recordingStartTime) / 1000L
                );

        try {

            if (audioRecorder != null) {
                audioRecorder.stopRecording();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Error stopping voice recorder",
                    e
            );

            shouldSend = false;
        }

        audioRecorder = null;

        hideRecordingOverlay();

        String recordedPath = currentAudioFile;

        currentAudioFile = null;

        if (recordedPath == null) {
            return;
        }

        File audioFile =
                new File(recordedPath);

        if (!shouldSend || duration < 1 || !audioFile.exists()) {

            if (audioFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                audioFile.delete();
            }

            if (shouldSend && duration < 1) {

                Toast.makeText(
                        this,
                        "Voice message is too short",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }

        uploadVoiceMessageAndSend(
                audioFile,
                duration
        );
    }

    private void uploadVoiceMessageAndSend(
            @NonNull File audioFile,
            int duration
    ) {

        if (chatId == null || !audioFile.exists()) {
            return;
        }

        Uri audioUri;

        try {

            audioUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            audioFile
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not create voice file URI",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to prepare voice message",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "Sending voice...",
                Toast.LENGTH_SHORT
        ).show();

        String fileName =
                "voice_" + UUID.randomUUID() + ".m4a";

        StorageReference storageRef =
                FirebaseStorage
                        .getInstance()
                        .getReference("chat_attachments")
                        .child(chatId)
                        .child(fileName);

        storageRef
                .putFile(audioUri)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {

                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Voice upload failed");
                    }

                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {

                    if (!isActivityAvailable()) {
                        return;
                    }

                    if (task.isSuccessful()
                            && task.getResult() != null) {

                        sendVoiceMessage(
                                task.getResult().toString(),
                                duration
                        );

                    } else {

                        Log.e(
                                TAG,
                                "Voice upload failed",
                                task.getException()
                        );

                        Toast.makeText(
                                this,
                                "Voice upload failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    //noinspection ResultOfMethodCallIgnored
                    audioFile.delete();
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

        ChatMessage message =
                new ChatMessage(
                        msgId,
                        currentUserId,
                        "Voice message",
                        "voice",
                        fileUrl,
                        timestamp
                );

        message.setVoiceDuration(duration);

        chatRef
                .child(msgId)
                .setValue(message)
                .addOnSuccessListener(aVoid -> {

                    updateChatMeta(
                            "Voice message",
                            timestamp,
                            "voice"
                    );
                })
                .addOnFailureListener(error -> {

                    Log.e(
                            TAG,
                            "Voice message database write failed",
                            error
                    );

                    Toast.makeText(
                            this,
                            "Could not send voice message",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // FIREBASE MESSAGE LISTENER
    // =========================================================

    private void setupFirebase() {

        if (chatId == null || chatId.isEmpty()) {
            return;
        }

        removeMessagesListener();

        try {

            chatRef =
                    FirebaseDatabase
                            .getInstance(FIREBASE_DB_URL)
                            .getReference("chats")
                            .child(chatId)
                            .child("messages");

            messagesQuery =
                    chatRef
                            .orderByKey()
                            .limitToLast(50);

            msgListener =
                    new ChildEventListener() {

                        @Override
                        public void onChildAdded(
                                @NonNull DataSnapshot snapshot,
                                @Nullable String previousChildName
                        ) {

                            if (!isActivityAvailable()) {
                                return;
                            }

                            ChatMessage message =
                                    snapshot.getValue(ChatMessage.class);

                            if (message == null || adapter == null) {
                                return;
                            }

                            String messageId = message.getId();

                            if (messageId == null) {
                                Log.w(
                                        TAG,
                                        "Message without ID received: "
                                                + snapshot.getKey()
                                );

                                return;
                            }

                            // Prevent duplicate insertion.
                            for (ChatMessage existing : messageList) {

                                if (existing != null
                                        && messageId.equals(existing.getId())) {

                                    return;
                                }
                            }

                            boolean wasAtBottom =
                                    recyclerView == null
                                            || !recyclerView.canScrollVertically(1);

                            messageList.add(message);

                            int position =
                                    messageList.size() - 1;

                            adapter.notifyItemInserted(position);

                            if (wasAtBottom && recyclerView != null) {

                                recyclerView.scrollToPosition(position);
                            }

                            // Mark incoming messages as read.
                            if (message.getSenderId() != null
                                    && !message.getSenderId().equals(currentUserId)
                                    && !message.isRead()) {

                                markSingleMessageAsRead(message);
                            }
                        }

                        @Override
                        public void onChildChanged(
                                @NonNull DataSnapshot snapshot,
                                @Nullable String previousChildName
                        ) {

                            if (!isActivityAvailable()) {
                                return;
                            }

                            ChatMessage updatedMessage =
                                    snapshot.getValue(ChatMessage.class);

                            if (updatedMessage == null
                                    || updatedMessage.getId() == null) {

                                return;
                            }

                            for (int i = 0;
                                 i < messageList.size();
                                 i++) {

                                ChatMessage localMessage =
                                        messageList.get(i);

                                if (localMessage != null
                                        && updatedMessage.getId().equals(
                                        localMessage.getId()
                                )) {

                                    // Replace the entire model, not only
                                    // its read property.
                                    messageList.set(
                                            i,
                                            updatedMessage
                                    );

                                    adapter.notifyItemChanged(i);

                                    break;
                                }
                            }
                        }

                        @Override
                        public void onChildRemoved(
                                @NonNull DataSnapshot snapshot
                        ) {

                            if (!isActivityAvailable()) {
                                return;
                            }

                            String removedId =
                                    snapshot.getKey();

                            if (removedId == null) {
                                return;
                            }

                            for (int i = 0;
                                 i < messageList.size();
                                 i++) {

                                ChatMessage message =
                                        messageList.get(i);

                                if (message != null
                                        && removedId.equals(message.getId())) {

                                    messageList.remove(i);

                                    adapter.notifyItemRemoved(i);

                                    break;
                                }
                            }
                        }

                        @Override
                        public void onChildMoved(
                                @NonNull DataSnapshot snapshot,
                                @Nullable String previousChildName
                        ) {

                            // Messages are ordered by their push keys.
                            // No local reorder is needed for this query.
                        }

                        @Override
                        public void onCancelled(
                                @NonNull DatabaseError error
                        ) {

                            Log.e(
                                    TAG,
                                    "Messages listener cancelled",
                                    error.toException()
                            );

                            if (isActivityAvailable()) {

                                Toast.makeText(
                                        ChatActivity.this,
                                        "Failed to load messages: "
                                                + error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    };

            messagesQuery.addChildEventListener(
                    msgListener
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Chat Firebase setup failed",
                    e
            );

            Toast.makeText(
                    this,
                    "Chat initialization error",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void removeMessagesListener() {

        if (messagesQuery != null
                && msgListener != null) {

            try {

                messagesQuery.removeEventListener(
                        msgListener
                );

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Could not remove message listener",
                        e
                );
            }
        }

        messagesQuery = null;
        msgListener = null;
    }

    // =========================================================
    // READ RECEIPTS
    // =========================================================

    private void markSingleMessageAsRead(
            @NonNull ChatMessage message
    ) {

        if (chatRef == null
                || currentUserId == null
                || message.getId() == null
                || message.getSenderId() == null
                || message.getSenderId().equals(currentUserId)
                || message.isRead()) {

            return;
        }

        // Optimistic local update.
        message.setRead(true);

        notifyMessageChanged(message.getId());

        chatRef
                .child(message.getId())
                .child("read")
                .setValue(true)
                .addOnFailureListener(error -> {

                    Log.e(
                            TAG,
                            "Failed to mark message as read",
                            error
                    );

                    // Restore local state if Firebase rejects the write.
                    message.setRead(false);

                    notifyMessageChanged(message.getId());
                });
    }

    private void notifyMessageChanged(String messageId) {

        if (adapter == null) {
            return;
        }

        for (int i = 0;
             i < messageList.size();
             i++) {

            ChatMessage message =
                    messageList.get(i);

            if (message != null
                    && messageId.equals(message.getId())) {

                adapter.notifyItemChanged(i);

                return;
            }
        }
    }

    // =========================================================
    // SEND TEXT MESSAGE
    // =========================================================

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

            Toast.makeText(
                    this,
                    "Chat is not ready",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String msgId =
                chatRef.push().getKey();

        if (msgId == null) {
            return;
        }

        long timestamp =
                System.currentTimeMillis();

        ChatMessage message =
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
                .setValue(message)
                .addOnSuccessListener(aVoid -> {

                    if ("text".equals(type)
                            && etMessage != null) {

                        etMessage.setText("");
                    }

                    updateChatMeta(
                            content,
                            timestamp,
                            type
                    );
                })
                .addOnFailureListener(error -> {

                    Log.e(
                            TAG,
                            "Message send failed",
                            error
                    );

                    if (isActivityAvailable()) {

                        Toast.makeText(
                                this,
                                "Message failed: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================================================
    // CHAT METADATA
    // =========================================================

    private void updateChatMeta(
            String lastMessage,
            long timestamp,
            String type
    ) {

        if (currentUserId == null
                || otherUserId == null
                || chatId == null) {

            return;
        }

        try {

            /*
             * IMPORTANT:
             *
             * RTDB Rules allow the authenticated user to write
             * only their own user_chats/{uid} branch.
             *
             * Therefore this client updates ONLY:
             *
             * user_chats/{currentUserId}/{otherUserId}
             *
             * Updating the recipient's branch must be handled
             * by a trusted backend function.
             */

            DatabaseReference ownChatRef =
                    FirebaseDatabase
                            .getInstance(FIREBASE_DB_URL)
                            .getReference("user_chats")
                            .child(currentUserId)
                            .child(otherUserId);

            Map<String, Object> updates =
                    new HashMap<>();

            updates.put("lastMessage", lastMessage);
            updates.put("timestamp", timestamp);
            updates.put("chatId", chatId);
            updates.put("lastMessageType", type);

            ownChatRef
                    .updateChildren(updates)
                    .addOnFailureListener(error -> {

                        Log.e(
                                TAG,
                                "Own chat metadata update failed",
                                error
                        );
                    });

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Chat metadata update exception",
                    e
            );
        }
    }

    // =========================================================
    // ATTACHMENT PICKER
    // =========================================================

    private void showAttachmentDialog() {

        String[] options = {
                "Image",
                "File"
        };

        new AlertDialog.Builder(this)
                .setTitle("Attach")
                .setItems(
                        options,
                        (dialog, which) -> {

                            Intent intent =
                                    new Intent(Intent.ACTION_GET_CONTENT);

                            intent.addCategory(
                                    Intent.CATEGORY_OPENABLE
                            );

                            if (which == 0) {

                                intent.setType("image/*");

                                intent.putExtra(
                                        Intent.EXTRA_MIME_TYPES,
                                        new String[]{
                                                "image/jpeg",
                                                "image/png",
                                                "image/webp",
                                                "image/gif"
                                        }
                                );

                            } else {

                                intent.setType("*/*");

                                intent.putExtra(
                                        Intent.EXTRA_MIME_TYPES,
                                        CHAT_ATTACHMENT_MIME_TYPES
                                );
                            }

                            try {

                                startActivityForResult(
                                        Intent.createChooser(
                                                intent,
                                                "Choose attachment"
                                        ),
                                        which == 0
                                                ? PICK_IMAGE
                                                : PICK_FILE
                                );

                            } catch (Exception e) {

                                Log.e(
                                        TAG,
                                        "File picker could not be opened",
                                        e
                                );

                                Toast.makeText(
                                        this,
                                        "No compatible file picker is available",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
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

        if (requestCode != PICK_IMAGE
                && requestCode != PICK_FILE) {

            return;
        }

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {

            return;
        }

        uploadFileAndSend(
                data.getData(),
                requestCode
        );
    }

    // =========================================================
    // ATTACHMENT UPLOAD
    // =========================================================

    private void uploadFileAndSend(
            @NonNull Uri uri,
            int requestCode
    ) {

        if (chatId == null) {
            return;
        }

        ContentResolver resolver =
                getContentResolver();

        String mimeType =
                resolver.getType(uri);

        if (!isSupportedChatAttachmentMimeType(mimeType)) {

            Toast.makeText(
                    this,
                    "Unsupported file type",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (requestCode == PICK_IMAGE
                && !mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {

            Toast.makeText(
                    this,
                    "Please select an image",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        long contentSize =
                getUriSize(uri);

        if (contentSize < 0) {

            Toast.makeText(
                    this,
                    "Unable to determine file size. Choose another file.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (contentSize > MAX_CHAT_ATTACHMENT_BYTES) {

            Toast.makeText(
                    this,
                    "Attachments must be 25 MB or smaller",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String displayName =
                getDisplayName(uri);

        if (displayName == null
                || displayName.trim().isEmpty()) {

            displayName = "File";
        }

        String safeDisplayName =
                sanitizeFileName(displayName);

        String storageFileName =
                UUID.randomUUID().toString();

        String extension =
                MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(mimeType);

        if (extension != null
                && !extension.isEmpty()) {

            storageFileName += "." + extension;
        }

        StorageReference storageRef =
                FirebaseStorage
                        .getInstance()
                        .getReference("chat_attachments")
                        .child(chatId)
                        .child(storageFileName);

        StorageMetadata metadata =
                new StorageMetadata.Builder()
                        .setContentType(mimeType)
                        .build();

        Toast.makeText(
                this,
                "Uploading...",
                Toast.LENGTH_SHORT
        ).show();

        final String finalDisplayName =
                safeDisplayName;

        final String finalType =
                mimeType.toLowerCase(Locale.ROOT).startsWith("image/")
                        ? "image"
                        : "file";

        storageRef
                .putFile(uri, metadata)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {

                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Upload failed");
                    }

                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {

                    if (!isActivityAvailable()) {
                        return;
                    }

                    if (task.isSuccessful()
                            && task.getResult() != null) {

                        String downloadUrl =
                                task.getResult().toString();

                        sendMessage(
                                finalType,
                                finalDisplayName,
                                downloadUrl
                        );

                        Toast.makeText(
                                this,
                                "Upload successful",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Exception error =
                                task.getException();

                        Log.e(
                                TAG,
                                "Attachment upload failed",
                                error
                        );

                        Toast.makeText(
                                this,
                                "Upload failed: "
                                        + (
                                        error != null
                                                ? error.getMessage()
                                                : "Unknown error"
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private boolean isSupportedChatAttachmentMimeType(
            String mimeType
    ) {

        if (mimeType == null) {
            return false;
        }

        return Arrays.asList(
                CHAT_ATTACHMENT_MIME_TYPES
        ).contains(
                mimeType.toLowerCase(Locale.ROOT)
        );
    }

    private long getUriSize(Uri uri) {

        try (Cursor cursor =
                     getContentResolver().query(
                             uri,
                             new String[]{
                                     OpenableColumns.SIZE
                             },
                             null,
                             null,
                             null
                     )) {

            if (cursor != null
                    && cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                OpenableColumns.SIZE
                        );

                if (index >= 0
                        && !cursor.isNull(index)) {

                    return cursor.getLong(index);
                }
            }

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "Could not read attachment size from provider",
                    e
            );
        }

        return -1;
    }

    private String getDisplayName(Uri uri) {

        try (Cursor cursor =
                     getContentResolver().query(
                             uri,
                             new String[]{
                                     OpenableColumns.DISPLAY_NAME
                             },
                             null,
                             null,
                             null
                     )) {

            if (cursor != null
                    && cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                if (index >= 0) {

                    return cursor.getString(index);
                }
            }

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "Could not read attachment display name",
                    e
            );
        }

        return null;
    }

    private String sanitizeFileName(String fileName) {

        if (fileName == null) {
            return "File";
        }

        String safe =
                fileName.replaceAll(
                        "[^a-zA-Z0-9._ -]",
                        "_"
                );

        safe = safe.trim();

        if (safe.isEmpty()
                || safe.equals(".")
                || safe.equals("..")) {

            return "File";
        }

        if (safe.length() > 100) {

            safe = safe.substring(0, 100);
        }

        return safe;
    }

    // =========================================================
    // OPEN DOWNLOADED FILE
    // =========================================================

    public void openFile(String fileUrl) {

        if (fileUrl == null
                || fileUrl.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid file URL",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final URL parsedUrl;

        try {

            parsedUrl = new URL(fileUrl);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Invalid file URL",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * Chat attachments must use HTTPS Firebase Storage URLs.
         * Do not open arbitrary URLs supplied by a message.
         */

        if (!"https".equalsIgnoreCase(parsedUrl.getProtocol())
                || !"firebasestorage.googleapis.com".equalsIgnoreCase(
                parsedUrl.getHost()
        )) {

            Toast.makeText(
                    this,
                    "Unsupported file location",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "Downloading file...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {

            HttpURLConnection connection = null;

            File tempFile = null;

            try {

                connection =
                        (HttpURLConnection) parsedUrl.openConnection();

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");

                // Do not silently follow redirects to other hosts.
                connection.setInstanceFollowRedirects(false);

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {

                    throw new IOException(
                            "Unexpected HTTP response: " + responseCode
                    );
                }

                long contentLength =
                        connection.getContentLengthLong();

                if (contentLength > MAX_CHAT_ATTACHMENT_BYTES) {

                    throw new IOException(
                            "File exceeds the 25 MB limit"
                    );
                }

                String originalName =
                        parsedUrl.getPath();

                if (originalName != null
                        && originalName.contains("/")) {

                    originalName =
                            originalName.substring(
                                    originalName.lastIndexOf('/') + 1
                            );
                }

                String extension = "";

                if (originalName != null) {

                    int dot =
                            originalName.lastIndexOf('.');

                    if (dot >= 0
                            && dot < originalName.length() - 1) {

                        extension =
                                originalName.substring(dot)
                                        .replaceAll(
                                                "[^a-zA-Z0-9.]",
                                                ""
                                        );
                    }
                }

                if (extension.isEmpty()
                        || extension.length() > 10) {

                    extension = ".bin";
                }

                tempFile =
                        File.createTempFile(
                                "chat_",
                                extension,
                                getCacheDir()
                        );

                long totalBytes = 0;

                try (
                        InputStream input =
                                connection.getInputStream();

                        FileOutputStream output =
                                new FileOutputStream(tempFile)
                ) {

                    byte[] buffer =
                            new byte[8192];

                    int bytesRead;

                    while ((bytesRead = input.read(buffer)) != -1) {

                        totalBytes += bytesRead;

                        if (totalBytes > MAX_CHAT_ATTACHMENT_BYTES) {

                            throw new IOException(
                                    "File exceeds the 25 MB limit"
                            );
                        }

                        output.write(
                                buffer,
                                0,
                                bytesRead
                        );
                    }

                    output.flush();
                }

                File downloadedFile = tempFile;

                runOnUiThread(() -> {

                    if (isActivityAvailable()) {

                        openLocalFile(downloadedFile);

                    } else {

                        //noinspection ResultOfMethodCallIgnored
                        downloadedFile.delete();
                    }
                });

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "File download failed",
                        e
                );

                if (tempFile != null
                        && tempFile.exists()) {

                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }

                runOnUiThread(() -> {

                    if (isActivityAvailable()) {

                        Toast.makeText(
                                ChatActivity.this,
                                "Download failed: "
                                        + (
                                        e.getMessage() != null
                                                ? e.getMessage()
                                                : "Unknown error"
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void openLocalFile(File file) {

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

            Uri uri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            file
                    );

            String mimeType =
                    getMimeType(file);

            if (mimeType == null
                    || mimeType.isEmpty()) {

                mimeType = "*/*";
            }

            Intent intent =
                    new Intent(Intent.ACTION_VIEW);

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

            Log.e(
                    TAG,
                    "Could not open downloaded file",
                    e
            );

            Toast.makeText(
                    this,
                    "Error opening file",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String getMimeType(File file) {

        if (file == null) {
            return null;
        }

        String extension =
                MimeTypeMap.getFileExtensionFromUrl(
                        file.getAbsolutePath()
                );

        if (extension == null
                || extension.isEmpty()) {

            return null;
        }

        return MimeTypeMap
                .getSingleton()
                .getMimeTypeFromExtension(
                        extension.toLowerCase(Locale.ROOT)
                );
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onDestroy() {

        removeMessagesListener();

        if (adapter != null) {

            try {

                adapter.stopPlaying();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Could not stop voice playback",
                        e
                );
            }
        }

        if (isRecordingAudio) {

            try {

                isRecordingAudio = false;

                if (audioRecorder != null) {
                    audioRecorder.stopRecording();
                }

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Could not stop audio recorder",
                        e
                );
            }
        }

        audioRecorder = null;

        if (recordingHandler != null) {

            recordingHandler.removeCallbacksAndMessages(null);
        }

        super.onDestroy();
    }
}
