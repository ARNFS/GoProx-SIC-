package com.example.goprox;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class FullscreenImageActivity extends AppCompatActivity {

    private PhotoView photoView;
    private ImageButton btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        photoView = findViewById(R.id.photoView);
        btnClose = findViewById(R.id.btnClose);

        if (photoView == null) {
            Toast.makeText(this, "UI error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(photoView);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finishSafe());
        }
        photoView.setOnClickListener(v -> finishSafe());
    }

    private void finishSafe() {
        if (!isFinishing() && !isDestroyed()) {
            finish();
        }
    }
}