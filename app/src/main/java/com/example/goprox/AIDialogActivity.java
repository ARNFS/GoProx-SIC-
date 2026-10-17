
package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.functions.FirebaseFunctions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AIDialogActivity extends BaseActivity {

    private EditText etUserInput;
    private Button btnSend;
    private LinearLayout llChatContainer;
    private ScrollView scrollView;

    private FirebaseFunctions firebaseFunctions;
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

        /*
         * Firebase Functions are now the only path to AI.
         *
         * The Groq API key is NOT stored in the Android app.
         */
        firebaseFunctions =
                FirebaseFunctions.getInstance("europe-west1");

        firebaseService = new FirebaseService();

        addMessage(
                "🔧 AI is ready. Describe what you need.",
                false,
                0
        );

        btnSend.setOnClickListener(v -> {

            String text =
                    etUserInput.getText()
                            .toString()
                            .trim();

            if (!text.isEmpty()) {

                addMessage(
                        text,
                        true,
                        0
                );

                etUserInput.setText("");

                analyzeProblem(text);
            }
        });

        etUserInput.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {

                        new Handler().postDelayed(
                                () ->
                                        scrollView.fullScroll(
                                                ScrollView.FOCUS_DOWN
                                        ),
                                150
                        );
                    }
                }
        );
    }

    private void analyzeProblem(String problem) {

        if (!canMakeRequest()) {

            addMessage(
                    "⏳ Please wait 1 second before sending another request.",
                    false,
                    0
            );

            return;
        }

        TextView thinking = new TextView(this);

        thinking.setText(
                "⏳ AI is analyzing..."
        );

        thinking.setPadding(
                24,
                16,
                24,
                16
        );

        thinking.setBackgroundResource(
                R.drawable.bg_ai_message
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                16,
                8,
                100,
                8
        );

        thinking.setLayoutParams(params);

        llChatContainer.addView(
                thinking
        );

        scrollView.post(
                () ->
                        scrollView.fullScroll(
                                ScrollView.FOCUS_DOWN
                        )
        );

        firebaseService.getAllServicesWithTags(
                services -> {

                    if (services == null || services.isEmpty()) {

                        runOnUiThread(() -> {

                            llChatContainer.removeView(
                                    thinking
                            );

                            addMessage(
                                    "❌ No professionals found. Please add services first.",
                                    false,
                                    0
                            );
                        });

                        return;
                    }

                    /*
                     * -------------------------------------------------
                     * Build bounded service catalog.
                     *
                     * The backend will validate everything again.
                     * -------------------------------------------------
                     */

                    ArrayList<Map<String, Object>> serviceData =
                            new ArrayList<>();

                    Set<String> knownTitles =
                            new HashSet<>();

                    Map<String, String> canonicalTitles =
                            new HashMap<>();

                    for (Service service : services) {

                        if (service == null) {
                            continue;
                        }

                        String title =
                                service.getProfession() != null
                                        ? service.getProfession().trim()
                                        : "";

                        String description =
                                service.getDescription() != null
                                        ? service.getDescription().trim()
                                        : "";

                        if (title.isEmpty()) {
                            continue;
                        }

                        Map<String, Object> serviceItem =
                                new HashMap<>();

                        serviceItem.put(
                                "title",
                                title
                        );

                        serviceItem.put(
                                "description",
                                description
                        );

                        serviceItem.put(
                                "rating",
                                service.getRating()
                        );

                        ArrayList<String> tags =
                                new ArrayList<>();

                        if (service.getTags() != null) {

                            for (String tag : service.getTags()) {

                                if (tag != null) {
                                    String cleanTag =
                                            tag.trim();

                                    if (!cleanTag.isEmpty()) {
                                        tags.add(cleanTag);
                                    }
                                }
                            }
                        }

                        serviceItem.put(
                                "tags",
                                tags
                        );

                        serviceData.add(
                                serviceItem
                        );

                        String normalized =
                                title.toLowerCase();

                        knownTitles.add(
                                normalized
                        );

                        canonicalTitles.put(
                                normalized,
                                title
                        );
                    }

                    if (serviceData.isEmpty()) {

                        runOnUiThread(() -> {

                            llChatContainer.removeView(
                                    thinking
                            );

                            addMessage(
                                    "❌ No usable professionals found.",
                                    false,
                                    0
                            );
                        });

                        return;
                    }

                    /*
                     * -------------------------------------------------
                     * ONLY normal user/catalog data is sent.
                     *
                     * NO Groq API key.
                     * NO direct provider request.
                     * -------------------------------------------------
                     */

                    Map<String, Object> requestData =
                            new HashMap<>();

                    requestData.put(
                            "problem",
                            problem
                    );

                    requestData.put(
                            "services",
                            serviceData
                    );

                    firebaseFunctions
                            .getHttpsCallable("askAI")
                            .call(requestData)
                            .addOnSuccessListener(result -> {

                                runOnUiThread(() -> {

                                    llChatContainer.removeView(
                                            thinking
                                    );

                                    try {

                                        Object data =
                                                result != null
                                                        ? result.getData()
                                                        : null;

                                        if (!(data instanceof Map)) {

                                            addMessage(
                                                    "❌ Invalid AI response.",
                                                    false,
                                                    0
                                            );

                                            return;
                                        }

                                        Map<?, ?> responseData =
                                                (Map<?, ?>) data;

                                        Object contentObject =
                                                responseData.get(
                                                        "content"
                                                );

                                        String raw =
                                                contentObject instanceof String
                                                        ? ((String) contentObject).trim()
                                                        : "";

                                        Log.d(
                                                "AIDialog",
                                                "AI structured response received."
                                        );

                                        parseAIResponse(
                                                raw,
                                                knownTitles,
                                                canonicalTitles
                                        );

                                    } catch (Exception e) {

                                        Log.e(
                                                "AIDialog",
                                                "Failed to process AI response.",
                                                e
                                        );

                                        addMessage(
                                                "❌ Failed to process AI response.",
                                                false,
                                                0
                                        );
                                    }
                                });
                            })
                            .addOnFailureListener(error -> {

                                Log.e(
                                        "AIDialog",
                                        "AI request failed.",
                                        error
                                );

                                runOnUiThread(() -> {

                                    llChatContainer.removeView(
                                            thinking
                                    );

                                    addMessage(
                                            "❌ AI request failed. Please try again later.",
                                            false,
                                            0
                                    );
                                });
                            });
                }
        );
    }

    private void parseAIResponse(
            String raw,
            Set<String> knownTitles,
            Map<String, String> canonicalTitles
    ) {

        if (raw == null || raw.trim().isEmpty()) {

            addMessage(
                    "😔 No matching professional found.",
                    false,
                    0
            );

            return;
        }

        try {

            JSONObject json =
                    new JSONObject(
                            raw.trim()
                    );

            String intent =
                    json.optString(
                            "intent",
                            "NON_SERVICE_REQUEST"
                    ).trim();

            int confidence =
                    json.optInt(
                            "confidence",
                            0
                    );

            if (confidence < 0) {
                confidence = 0;
            }

            if (confidence > 100) {
                confidence = 100;
            }

            /*
             * -------------------------------------------------
             * IMPORTANT:
             *
             * AI must first decide whether the user is actually
             * looking for a professional service.
             *
             * Example:
             *
             * "I want to play a game"
             * -> NON_SERVICE_REQUEST
             *
             * "I need a game developer"
             * -> SERVICE_SEARCH
             * -------------------------------------------------
             */

            if (!"SERVICE_SEARCH".equalsIgnoreCase(intent)) {

                addMessage(
                        "😔 No matching professional found.",
                        false,
                        0
                );

                return;
            }

            if (
                    !json.has("matched_service_title")
                            || json.isNull("matched_service_title")
            ) {

                addMessage(
                        "😔 No matching professional found.",
                        false,
                        0
                );

                return;
            }

            String returnedTitle =
                    json.optString(
                            "matched_service_title",
                            ""
                    ).trim();

            if (returnedTitle.isEmpty()) {

                addMessage(
                        "😔 No matching professional found.",
                        false,
                        0
                );

                return;
            }

            /*
             * -------------------------------------------------
             * Defense in depth:
             *
             * Even if backend returns something unexpected,
             * Android accepts only a profession that actually
             * exists in the catalog sent to the backend.
             * -------------------------------------------------
             */

            String normalizedReturnedTitle =
                    returnedTitle.toLowerCase();

            if (!knownTitles.contains(
                    normalizedReturnedTitle
            )) {

                Log.w(
                        "AIDialog",
                        "AI returned a title that is not in catalog: "
                                + returnedTitle
                );

                addMessage(
                        "😔 No matching professional found.",
                        false,
                        0
                );

                return;
            }

            String canonicalTitle =
                    canonicalTitles.get(
                            normalizedReturnedTitle
                    );

            if (
                    canonicalTitle == null
                            || canonicalTitle.trim().isEmpty()
            ) {

                addMessage(
                        "😔 No matching professional found.",
                        false,
                        0
                );

                return;
            }

            /*
             * -------------------------------------------------
             * One exact service match.
             * -------------------------------------------------
             */

            ArrayList<String> finalTitles =
                    new ArrayList<>();

            finalTitles.add(
                    canonicalTitle
            );

            String resultMessage =
                    "✅ Found:\n"
                            + canonicalTitle
                            + ": "
                            + confidence
                            + "% match";

            addMessage(
                    resultMessage,
                    false,
                    0
            );

            String ratingStars =
                    getStarRating(
                            confidence
                    );

            addMessage(
                    "📊 AI Confidence: "
                            + confidence
                            + "% "
                            + ratingStars,
                    false,
                    confidence
            );

            Intent navigationIntent =
                    new Intent(
                            AIDialogActivity.this,
                            HomeActivity.class
                    );

            navigationIntent.putStringArrayListExtra(
                    "profession_filter_list",
                    finalTitles
            );

            navigationIntent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(navigationIntent);

            finish();

        } catch (Exception e) {

            Log.e(
                    "AIDialog",
                    "Invalid structured AI response: " + raw,
                    e
            );

            addMessage(
                    "❌ Invalid AI response.",
                    false,
                    0
            );
        }
    }

    private String getStarRating(
            int confidence
    ) {

        if (confidence >= 90) {
            return "⭐⭐⭐⭐⭐";
        }

        if (confidence >= 70) {
            return "⭐⭐⭐⭐";
        }

        if (confidence >= 50) {
            return "⭐⭐⭐";
        }

        if (confidence >= 30) {
            return "⭐⭐";

        }

        return "⭐";
    }

    private void addMessage(
            String text,
            boolean isUser,
            int confidence
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(text);
        tv.setTextSize(16);

        tv.setPadding(
                24,
                16,
                24,
                16
        );

        tv.setBackgroundResource(
                isUser
                        ? R.drawable.bg_user_message
                        : R.drawable.bg_ai_message
        );

        tv.setTextColor(
                isUser
                        ? 0xFFFFFFFF
                        : 0xFF000000
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                isUser ? 100 : 16,
                8,
                isUser ? 16 : 100,
                8
        );

        params.gravity =
                isUser
                        ? android.view.Gravity.END
                        : android.view.Gravity.START;

        tv.setLayoutParams(params);

        tv.setMaxWidth(900);

        llChatContainer.addView(
                tv
        );

        scrollView.post(
                () ->
                        scrollView.fullScroll(
                                ScrollView.FOCUS_DOWN
                        )
        );
    }

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();

        return true;
    }
}
