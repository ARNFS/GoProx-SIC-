package com.example.goprox;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIDialogActivity extends AppCompatActivity {

    private EditText etUserInput;
    private Button btnSend;
    private LinearLayout llChatContainer;
    private ScrollView scrollView;

    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_dialog);

        etUserInput = findViewById(R.id.etUserInput);
        btnSend = findViewById(R.id.btnSend);
        llChatContainer = findViewById(R.id.llChatContainer);
        scrollView = findViewById(R.id.scrollView);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Assistant");
        }

        addMessage("🔧 AI is ready! Describe your problem", false);

        btnSend.setOnClickListener(v -> {
            String text = etUserInput.getText().toString().trim();
            if (!text.isEmpty()) {
                addMessage(text, true);
                etUserInput.setText("");
                askGroq(text);
            }
        });

        // ✅ Auto-scroll when keyboard opens
        etUserInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                new Handler().postDelayed(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN), 150);
            }
        });
    }

    private void askGroq(String question) {
        // Show thinking message
        TextView thinking = new TextView(this);
        thinking.setText("⏳ AI is thinking...");
        thinking.setPadding(16, 16, 16, 16);
        thinking.setBackgroundResource(R.drawable.bg_ai_message);
        LinearLayout.LayoutParams thinkingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        thinkingParams.setMargins(16, 8, 100, 8);
        thinking.setLayoutParams(thinkingParams);
        llChatContainer.addView(thinking);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        try {
            JSONObject json = new JSONObject();
            json.put("model", "llama-3.3-70b-versatile");
            json.put("temperature", 0.7);

            JSONArray messages = new JSONArray();

            // System prompt
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "You are a helpful assistant for GoProx app. " +
                            "Based on the user's problem, tell them what profession they need. " +
                            "Answer with ONE profession word only. " +
                            "Examples:\n" +
                            "- 'I need a website' → Web Developer\n" +
                            "- 'washing machine broken' → Electrician\n" +
                            "- 'water leak' → Plumber\n" +
                            "- 'car won't start' → Auto Mechanic\n" +
                            "- 'house cleaning' → Cleaner\n" +
                            "- 'my phone is hacked' → Cybersecurity Engineer"
            );
            messages.put(systemMsg);

            // User message
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            messages.put(userMsg);

            json.put("messages", messages);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        llChatContainer.removeView(thinking);
                        addMessage("❌ Network error: " + e.getMessage(), false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body().string();
                    runOnUiThread(() -> {
                        llChatContainer.removeView(thinking);
                        try {
                            if (!response.isSuccessful()) {
                                addMessage("❌ API Error " + response.code() + ":\n" + resBody, false);
                                return;
                            }

                            JSONObject obj = new JSONObject(resBody);
                            String answer = obj.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            addMessage("🔧 **Profession:** " + answer, false);

                        } catch (Exception e) {
                            addMessage("❌ Error parsing:\n" + resBody, false);
                        }
                    });
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                llChatContainer.removeView(thinking);
                addMessage("❌ Error: " + e.getMessage(), false);
            });
        }
    }

    private void addMessage(String text, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(24, 16, 24, 16);
        tv.setBackgroundResource(isUser ? R.drawable.bg_user_message : R.drawable.bg_ai_message);
        tv.setTextColor(isUser ? 0xFFFFFFFF : 0xFF000000);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(isUser ? 100 : 16, 8, isUser ? 16 : 100, 8);
        params.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
        tv.setLayoutParams(params);
        tv.setMaxWidth(800);
        llChatContainer.addView(tv);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}