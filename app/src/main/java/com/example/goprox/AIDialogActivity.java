package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
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
import java.util.List;

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

    // ✅ MOVED API KEY (you should move this to BuildConfig for security)
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private OkHttpClient client = new OkHttpClient();
    private FirebaseService firebaseService;

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

        firebaseService = new FirebaseService();

        addMessage("🔧 AI is ready! Describe your problem in any language.", false);

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
        TextView thinking = new TextView(this);
        thinking.setText("⏳ AI is analyzing...");
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

        firebaseService.getAllServicesWithTags(services -> {
            if (services.isEmpty()) {
                runOnUiThread(() -> {
                    llChatContainer.removeView(thinking);
                    addMessage("❌ No professionals found. Please add services first.", false);
                });
                return;
            }

            // Build professional list with tags and descriptions
            StringBuilder professionalsList = new StringBuilder();
            for (Service s : services) {
                professionalsList.append("• **")
                        .append(s.getProfession())
                        .append("**\n")
                        .append("  Description: ").append(s.getDescription()).append("\n");

                if (s.getTags() != null && !s.getTags().isEmpty()) {
                    professionalsList.append("  Skills/Tags: ")
                            .append(TextUtils.join(", ", s.getTags()))
                            .append("\n");
                }
                professionalsList.append("\n");
            }

            // 🔥 ULTRA-DETAILED PROMPT WITH SEMANTIC UNDERSTANDING
            String prompt =
                    "================================================================================\n" +
                            "YOU ARE A HIGHLY INTELLIGENT PROFESSIONAL MATCHMAKER FOR GoProx APP.\n" +
                            "================================================================================\n\n" +
                            "Your task: Given a user's request and a list of professionals (each with title, description, and skills/tags), you must choose the SINGLE MOST SUITABLE professional.\n\n" +
                            "RULES:\n" +
                            "1. Read the user's request carefully. Understand its MEANING, not just keywords.\n" +
                            "2. A professional can match even if their title doesn't exactly match, if their DESCRIPTION or TAGS show they can do the work.\n" +
                            "3. Example: 'PHP Programmer' with description 'making php scripts and servers' and tags 'php, server, backend' should be matched when user says 'I need a server' or 'I need backend work'.\n" +
                            "4. Example: 'Electrician' with description 'repairing switches, wiring' should be matched when user says 'my lights aren't working' or 'electrical problem'.\n" +
                            "5. If multiple professionals match, choose the one whose description and tags are most specific and relevant.\n" +
                            "6. If NO professional's description or tags match the user's request, answer 'NOT_FOUND'.\n" +
                            "7. Do NOT guess. Do NOT select a professional if their description does not explicitly cover the task.\n" +
                            "8. Return ONLY the exact profession title as it appears in the list, or 'NOT_FOUND'.\n" +
                            "9. No explanations, no extra text, no lists.\n\n" +
                            "================================================================================\n" +
                            "EXAMPLES OF SEMANTIC MATCHING (read and learn):\n" +
                            "================================================================================\n\n" +
                            "User: 'I need a server' | Professional: 'PHP Programmer' (tags: php, server, backend) → MATCH: 'PHP Programmer'\n" +
                            "User: 'I need a website' | Professional: 'Web Developer' (tags: html, css, javascript) → MATCH: 'Web Developer'\n" +
                            "User: 'My car won't start' | Professional: 'Auto Mechanic' (tags: car, engine, repair) → MATCH: 'Auto Mechanic'\n" +
                            "User: 'I want an iPhone app' | Professional: 'iOS Developer' (tags: ios, swift, iphone) → MATCH: 'iOS Developer'\n" +
                            "User: 'Electrical problem at home' | Professional: 'Electrician' (tags: electrical, wiring) → MATCH: 'Electrician'\n" +
                            "User: 'Need plumbing repair' | Professional: 'Plumber' (tags: pipe, leak, water) → MATCH: 'Plumber'\n" +
                            "User: 'I need a cross-platform mobile app' | Professional with tags 'ios, swift' only → NOT_FOUND (no cross-platform expert)\n\n" +
                            "================================================================================\n" +
                            "NOW PROCESS THE FOLLOWING USER REQUEST:\n" +
                            "================================================================================\n\n" +
                            "User request: \"" + problem + "\"\n\n" +
                            "Here is the list of available professionals (title + description + skills/tags):\n\n" +
                            professionalsList.toString() +
                            "================================================================================\n" +
                            "YOUR ANSWER (ONLY profession title or NOT_FOUND):\n" +
                            "Profession:";

            try {
                JSONObject json = new JSONObject();
                json.put("model", "llama-3.3-70b-versatile");
                json.put("temperature", 0.1);
                json.put("max_tokens", 50);

                JSONArray messages = new JSONArray();
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
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
                                String profession = obj.getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content")
                                        .trim()
                                        .replace("Profession:", "")
                                        .replace("\"", "")
                                        .trim();

                                // Clean up the response
                                if (profession.startsWith("**") && profession.endsWith("**")) {
                                    profession = profession.substring(2, profession.length() - 2);
                                }

                                addMessage("🔧 **Recommended:** " + profession, false);

                                if (profession.equalsIgnoreCase("NOT_FOUND")) {
                                    addMessage("I couldn't find a matching professional. Try describing differently.", false);
                                    return;
                                }

                                // Navigate to Home with filter
                                Intent intent = new Intent(AIDialogActivity.this, HomeActivity.class);
                                intent.putExtra("profession_filter", profession);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                addMessage("❌ Error parsing:\n" + resBody, false);
                                e.printStackTrace();
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