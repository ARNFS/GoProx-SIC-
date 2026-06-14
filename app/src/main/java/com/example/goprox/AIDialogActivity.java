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

import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIDialogActivity extends BaseActivity {

    private EditText etUserInput;
    private Button btnSend;
    private LinearLayout llChatContainer;
    private ScrollView scrollView;

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

        addMessage("🔧 AI is ready. Describe what you need.", false, 0);

        btnSend.setOnClickListener(v -> {
            String text = etUserInput.getText().toString().trim();
            if (!text.isEmpty()) {
                addMessage(text, true, 0);
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
            addMessage("⏳ Please wait 1 second before sending another request.", false, 0);
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
                    addMessage("❌ No professionals found. Please add services first.", false, 0);
                });
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Service s : services) {
                sb.append("TITLE: ").append(s.getProfession()).append("\n");
                sb.append("DESCRIPTION: ").append(s.getDescription()).append("\n");
                sb.append("RATING: ").append(s.getRating()).append("/5\n");
                if (s.getTags() != null && !s.getTags().isEmpty()) {
                    sb.append("TAGS: ").append(TextUtils.join(", ", s.getTags())).append("\n");
                }
                sb.append("\n");
            }

            // 🔥 Ավելի խելացի prompt
            String prompt = "You are an expert matchmaker for a service-finding app called GoProx.\n" +
                    "Your job is to find the BEST matching professionals for the user's request.\n\n" +
                    "RULES:\n" +
                    "1. Return ONLY the TITLE(s) that match, separated by commas\n" +
                    "2. If NO match at all, return exactly: NOT_FOUND\n" +
                    "3. Consider synonyms (e.g. \"plumber\" = \"pipe repair\", \"electrician\" = \"wiring\")\n" +
                    "4. Consider the user's INTENT, not just keywords\n" +
                    "5. Rate your confidence in each match from 0-100%\n\n" +
                    "USER REQUEST: \"" + problem + "\"\n\n" +
                    "AVAILABLE PROFESSIONALS:\n" + sb.toString() +
                    "FORMAT: Return in format: TITLE1|CONFIDENCE1, TITLE2|CONFIDENCE2\n" +
                    "Example: Plumber|95, Electrician|40\n" +
                    "If none match: NOT_FOUND\n\n" +
                    "Your answer:";

            try {
                JSONObject json = new JSONObject();
                json.put("model", "llama-3.3-70b-versatile");
                json.put("temperature", 0.1);
                json.put("max_tokens", 150);

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
                            addMessage("❌ Network error: " + e.getMessage(), false, 0);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String resBody = response.body().string();
                        runOnUiThread(() -> {
                            llChatContainer.removeView(thinking);
                            try {
                                if (!response.isSuccessful()) {
                                    addMessage("❌ API error: " + response.code(), false, 0);
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
                                    addMessage("😔 No matching professional found.", false, 0);
                                    return;
                                }

                                // Parse format: "TITLE1|CONFIDENCE1, TITLE2|CONFIDENCE2"
                                String[] parts = raw.split(",");
                                ArrayList<String> finalTitles = new ArrayList<>();
                                StringBuilder confidenceInfo = new StringBuilder();

                                for (String p : parts) {
                                    String[] titleConf = p.trim().split("\\|");
                                    if (titleConf.length >= 2) {
                                        String title = titleConf[0].trim();
                                        String confidence = titleConf[1].trim().replaceAll("[^0-9]", "");
                                        if (!title.isEmpty() && !confidence.isEmpty()) {
                                            finalTitles.add(title);
                                            int conf = Integer.parseInt(confidence);
                                            confidenceInfo.append(title).append(": ").append(conf).append("% match\n");
                                        }
                                    } else {
                                        String t = p.trim();
                                        if (!t.isEmpty()) finalTitles.add(t);
                                    }
                                }

                                if (finalTitles.isEmpty()) {
                                    addMessage("😔 No matching professional found.", false, 0);
                                    return;
                                }

                                // 🔥 Ցույց ենք տալիս արդյունքները + վստահության %
                                String resultMessage = "✅ Found:\n" + confidenceInfo.toString();
                                addMessage(resultMessage, false, 0);

                                // 🔥 AI-ի գնահատականը (average confidence)
                                int avgConfidence = calculateAverageConfidence(confidenceInfo.toString());
                                String ratingStars = getStarRating(avgConfidence);
                                addMessage("📊 AI Confidence: " + avgConfidence + "% " + ratingStars, false, 0);

                                Intent intent = new Intent(AIDialogActivity.this, HomeActivity.class);
                                intent.putStringArrayListExtra("profession_filter_list", finalTitles);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                addMessage("❌ Error: " + e.getMessage(), false, 0);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    llChatContainer.removeView(thinking);
                    addMessage("❌ Error: " + e.getMessage(), false, 0);
                });
            }
        });
    }

    // 🔥 Calculate average confidence
    private int calculateAverageConfidence(String confidenceInfo) {
        try {
            String[] lines = confidenceInfo.split("\n");
            int total = 0, count = 0;
            for (String line : lines) {
                String[] parts = line.split(": ");
                if (parts.length >= 2) {
                    String num = parts[1].replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        total += Integer.parseInt(num);
                        count++;
                    }
                }
            }
            return count > 0 ? total / count : 0;
        } catch (Exception e) { return 0; }
    }

    // 🔥 Star rating based on confidence
    private String getStarRating(int confidence) {
        if (confidence >= 90) return "⭐⭐⭐⭐⭐";
        if (confidence >= 70) return "⭐⭐⭐⭐";
        if (confidence >= 50) return "⭐⭐⭐";
        if (confidence >= 30) return "⭐⭐";
        return "⭐";
    }

    private void addMessage(String text, boolean isUser, int confidence) {
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