package com.example.goprox;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
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
        btnSubmit.setOnClickListener(v -> submitComplaint());
    }

    private void submitComplaint() {
        String reason = etReason.getText().toString().trim();
        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> complaint = new HashMap<>();
        complaint.put("serviceId", serviceId);
        complaint.put("reporterId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        complaint.put("targetUserId", targetUserId);
        complaint.put("reason", reason);
        complaint.put("timestamp", System.currentTimeMillis());
        db.collection("complaints").add(complaint)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Complaint sent", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show());
    }
}