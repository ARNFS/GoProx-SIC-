package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
/*ch*/
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private OkHttpClient client;
    private FirebaseService firebaseService;

    private long lastRequestTime = 0;
    private static final long MIN_INTERVAL = 1000;

    private boolean canMakeRequest() {
        long now = System.currentTimeMillis();
        if (now - lastRequestTime >= MIN_INTERVAL) {
            lastRequestTime = now;
            return true;
        }
        return false;
    }

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

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        firebaseService = new FirebaseService();

        addMessage("🔧 AI is ready. Describe what you need.", false);

        btnSend.setOnClickListener(v -> {
            String text = etUserInput.getText().toString().trim();
            if (!text.isEmpty()) {
                addMessage(text, true);
                etUserInput.setText("");
                analyzeProblem(text);
            }
        });

        etUserInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                new Handler().postDelayed(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN), 150);
            }
        });
    }

    private void analyzeProblem(String problem) {
        if (!canMakeRequest()) {
            addMessage("⏳ Please wait 1 second before sending another request.", false);
            return;
        }

        TextView thinking = new TextView(this);
        thinking.setText("⏳ AI is analyzing...");
        thinking.setPadding(24, 16, 24, 16);
        thinking.setBackgroundResource(R.drawable.bg_ai_message);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 100, 8);
        thinking.setLayoutParams(params);
        llChatContainer.addView(thinking);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        firebaseService.getAllServicesWithTags(services -> {
            if (services.isEmpty()) {
                runOnUiThread(() -> {
                    llChatContainer.removeView(thinking);
                    addMessage("❌ No professionals found. Please add services first.", false);
                });
                return;
            }

            // Build professional list for AI
            StringBuilder sb = new StringBuilder();
            for (Service s : services) {
                sb.append("TITLE: ").append(s.getProfession()).append("\n");
                sb.append("DESCRIPTION: ").append(s.getDescription()).append("\n");
                if (s.getTags() != null && !s.getTags().isEmpty()) {
                    sb.append("TAGS: ").append(TextUtils.join(", ", s.getTags())).append("\n");
                }
                sb.append("\n");
            }

            String prompt = "You are a precise matchmaker.\n\n" +
                    "USER REQUEST: \"" + problem + "\"\n\n" +
                    "AVAILABLE PROFESSIONALS:\n" + sb.toString() +
                    "Return ONLY the TITLE(s) that best match the user's request, separated by commas.\n" +
                    "If none match, return NOT_FOUND.\n\n" +
                    "Your answer:";

            try {
                JSONObject json = new JSONObject();
                json.put("model", "llama-3.3-70b-versatile");
                json.put("temperature", 0.1);
                json.put("max_tokens", 100);

                JSONArray messages = new JSONArray();
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);
                json.put("messages", messages);

                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .header("Authorization", "Bearer " + GROQ_API_KEY)
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
                                    addMessage("❌ API error: " + response.code(), false);
                                    return;
                                }

                                JSONObject obj = new JSONObject(resBody);
                                String raw = obj.getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content")
                                        .trim();

                                Log.d("AIDialog", "AI raw: " + raw);
                                if (raw.isEmpty() || raw.equalsIgnoreCase("NOT_FOUND")) {
                                    addMessage("😔 No matching professional found.", false);
                                    return;
                                }

                                String[] parts = raw.split(",");
                                ArrayList<String> aiTitles = new ArrayList<>();
                                for (String p : parts) {
                                    String t = p.trim();
                                    if (!t.isEmpty()) aiTitles.add(t);
                                }

                                // Platform filter
                                String lowerProblem = problem.toLowerCase();
                                boolean wantIos = lowerProblem.contains("iphone") || lowerProblem.contains("ios") || lowerProblem.contains("apple");
                                boolean wantAndroid = lowerProblem.contains("android") || lowerProblem.contains("google play");

                                ArrayList<String> finalTitles = new ArrayList<>();
                                for (String title : aiTitles) {
                                    Service matched = null;
                                    for (Service s : services) {
                                        if (s.getProfession().equalsIgnoreCase(title)) {
                                            matched = s;
                                            break;
                                        }
                                    }
                                    if (matched == null) continue;

                                    String fullText = (matched.getProfession() + " " + matched.getDescription()).toLowerCase();
                                    if (matched.getTags() != null) {
                                        for (String tag : matched.getTags()) fullText += " " + tag.toLowerCase();
                                    }

                                    boolean keep = true;
                                    if (wantIos && !(fullText.contains("ios") || fullText.contains("iphone") || fullText.contains("swift"))) keep = false;
                                    if (wantAndroid && !(fullText.contains("android") || fullText.contains("java") || fullText.contains("kotlin"))) keep = false;
                                    if (keep) finalTitles.add(title);
                                }

                                if (finalTitles.isEmpty()) {
                                    addMessage("😔 No matching professional found after filtering.", false);
                                    return;
                                }

                                addMessage("✅ Found: " + TextUtils.join(", ", finalTitles), false);
                                Intent intent = new Intent(AIDialogActivity.this, HomeActivity.class);
                                intent.putStringArrayListExtra("profession_filter_list", finalTitles);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                addMessage("❌ Error: " + e.getMessage(), false);
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
        });
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
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(isUser ? 100 : 16, 8, isUser ? 16 : 100, 8);
        params.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
        tv.setLayoutParams(params);
        tv.setMaxWidth(900);
        llChatContainer.addView(tv);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}