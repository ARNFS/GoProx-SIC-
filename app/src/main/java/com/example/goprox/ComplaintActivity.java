package com.example.goprox;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ComplaintActivity extends AppCompatActivity {
    private EditText etReason;
    private Button btnSubmit;
    private String serviceId, targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        serviceId = getIntent().getStringExtra("serviceId");
        targetUserId = getIntent().getStringExtra("targetUserId");

        etReason = findViewById(R.id.etReason);
        btnSubmit = findViewById(R.id.btnSubmit);

        if (etReason == null || btnSubmit == null) {
            Toast.makeText(this, "UI error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (serviceId == null || targetUserId == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSubmit.setOnClickListener(v -> submitComplaint());
    }

    private void submitComplaint() {
        if (etReason == null) return;

        String reason = etReason.getText().toString().trim();
        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> complaint = new HashMap<>();
        complaint.put("serviceId", serviceId);
        complaint.put("reporterId", currentUser.getUid());
        complaint.put("targetUserId", targetUserId);
        complaint.put("reason", reason);
        complaint.put("timestamp", System.currentTimeMillis());

        db.collection("complaints").add(complaint)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Complaint sent", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    String error = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
    }
}