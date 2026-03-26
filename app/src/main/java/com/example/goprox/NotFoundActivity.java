package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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

        String problem = getIntent().getStringExtra("problem");
        String profession = getIntent().getStringExtra("profession");

        if (problem != null) tvProblem.setText("Problem: " + problem);
        if (profession != null) tvProfession.setText("Needed: " + profession);

        btnBrowseAll.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnGoBack.setOnClickListener(v -> finish());
    }
}