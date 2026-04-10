package com.example.goprox;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

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

        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(photoView);
        }

        btnClose.setOnClickListener(v -> finish());
        photoView.setOnClickListener(v -> finish());
    }
}