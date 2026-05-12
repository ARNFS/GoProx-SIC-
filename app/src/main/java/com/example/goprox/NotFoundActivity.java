package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NotFoundActivity extends AppCompatActivity {

    private TextView tvProblem, tvProfession;
    private Button btnBrowseAll, btnGoBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_not_found);

        tvProblem = findViewById(R.id.tvProblem);
        tvProfession = findViewById(R.id.tvProfession);
        btnBrowseAll = findViewById(R.id.btnBrowseAll);
        btnGoBack = findViewById(R.id.btnGoBack);

        if (btnBrowseAll == null || btnGoBack == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String problem = getIntent().getStringExtra("problem");
        String profession = getIntent().getStringExtra("profession");

        if (tvProblem != null && problem != null) {
            tvProblem.setText("Problem: " + problem);
        }
        if (tvProfession != null && profession != null) {
            tvProfession.setText("Needed: " + profession);
        }

        btnBrowseAll.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, HomeActivity.class));
            } catch (Exception ignored) {}
            finish();
        });

        btnGoBack.setOnClickListener(v -> finish());
    }
}