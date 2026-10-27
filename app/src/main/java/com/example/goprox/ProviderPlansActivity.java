package com.example.goprox;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProviderPlansActivity extends AppCompatActivity {
    private final Map<String, PlanOffer> offersByProductId = new HashMap<>();
    private final Map<String, Button> buttonsByProductId = new HashMap<>();
    private BillingClient billingClient;
    private LinearLayout planCards;
    private TextView statusText;
    private String activePlanId;
    private long activeExpiryMillis;
    private int activePromotionCount;
    private boolean billingLaunchReady;
    private String serviceIdToPromote;
    private boolean promotionRequested;
    private boolean productQueryComplete;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_plans);

        Toolbar toolbar = findViewById(R.id.toolbarProviderPlans);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        planCards = findViewById(R.id.llProviderPlanCards);
        statusText = findViewById(R.id.tvProviderPlanStatus);
        serviceIdToPromote = getIntent().getStringExtra("serviceId");

        Button restoreButton = findViewById(R.id.btnRestoreProviderPlan);
        if (restoreButton != null) restoreButton.setOnClickListener(v -> restorePurchases());
        Button manageButton = findViewById(R.id.btnManageProviderPlan);
        if (manageButton != null) manageButton.setOnClickListener(v -> openPlaySubscriptions());
        Button termsButton = findViewById(R.id.btnProviderPlanTerms);
        Button privacyButton = findViewById(R.id.btnProviderPlanPrivacy);
        Button supportButton = findViewById(R.id.btnProviderPlanSupport);
        if (termsButton != null) termsButton.setOnClickListener(v -> openLegalUrl(termsButton.getTag()));
        if (privacyButton != null) privacyButton.setOnClickListener(v -> openLegalUrl(privacyButton.getTag()));
        if (supportButton != null) supportButton.setOnClickListener(v -> openLegalUrl(supportButton.getTag()));

        renderPlanCards();
        GoProxAnalytics.logProviderPlansOpened(this);
        refreshEntitlement();
        checkBillingReadiness();
        connectBilling();
    }

    private void connectBilling() {
        if (billingClient != null) {
            if (billingClient.isReady()) {
                queryProducts();
                queryOwnedPurchases();
                return;
            }
            billingClient.endConnection();
        }
        billingClient = BillingClient.newBuilder(this)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProducts();
                    queryOwnedPurchases();
                } else {
                    productQueryComplete = true;
                    updatePlanButtons();
                    setStatus("Google Play plans are unavailable right now. Try again later; no charge was made.");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                setStatus("Connection to Google Play was interrupted. Try again shortly.");
            }
        });
    }

    private void checkBillingReadiness() {
        FirebaseFunctions.getInstance("europe-west1")
                .getHttpsCallable("getGoProxBillingReadiness")
                .call()
                .addOnSuccessListener(result -> {
                    Object raw = result.getData();
                    if (!(raw instanceof Map)) {
                        setBillingReadiness(false, null, null, null);
                        return;
                    }
                    Map<?, ?> data = (Map<?, ?>) raw;
                    boolean ready = Boolean.TRUE.equals(data.get("ready"));
                    setBillingReadiness(ready, data.get("termsUrl"),
                            data.get("privacyUrl"), data.get("supportUrl"));
                })
                .addOnFailureListener(error -> {
                    setBillingReadiness(false, null, null, null);
                    setStatus("Plan purchases are unavailable until billing setup is verified. No charge can be made.");
                });
    }

    private void setBillingReadiness(boolean ready, Object terms, Object privacy, Object support) {
        String termsLink = httpsUrlOrNull(terms);
        String privacyLink = httpsUrlOrNull(privacy);
        String supportLink = httpsUrlOrNull(support);
        billingLaunchReady = ready && termsLink != null && privacyLink != null && supportLink != null;

        Button termsButton = findViewById(R.id.btnProviderPlanTerms);
        Button privacyButton = findViewById(R.id.btnProviderPlanPrivacy);
        Button supportButton = findViewById(R.id.btnProviderPlanSupport);
        if (termsButton != null) {
            termsButton.setTag(termsLink);
            termsButton.setVisibility(billingLaunchReady ? View.VISIBLE : View.GONE);
        }
        if (privacyButton != null) {
            privacyButton.setTag(privacyLink);
            privacyButton.setVisibility(billingLaunchReady ? View.VISIBLE : View.GONE);
        }
        if (supportButton != null) {
            supportButton.setTag(supportLink);
            supportButton.setVisibility(billingLaunchReady ? View.VISIBLE : View.GONE);
        }

        if (!billingLaunchReady && activePlanId == null) {
            setStatus("Provider plan setup is not complete yet. Purchases are disabled; no charge can be made.");
        } else if (billingLaunchReady && activePlanId == null && productQueryComplete) {
            setStatus(offersByProductId.isEmpty()
                    ? "These plans are not available in Google Play for this account yet. No charge can be made."
                    : "Choose a monthly plan. Google Play will show the final localized price before purchase.");
        }
        updatePlanButtons();
    }

    private String httpsUrlOrNull(Object value) {
        if (!(value instanceof String)) return null;
        Uri uri = Uri.parse((String) value);
        return "https".equalsIgnoreCase(uri.getScheme()) ? (String) value : null;
    }

    private void openLegalUrl(Object value) {
        String url = httpsUrlOrNull(value);
        if (url == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "This link could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (ProviderPlanCatalog.Plan plan : ProviderPlanCatalog.PLANS) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(plan.productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build());
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            productQueryComplete = true;
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && result != null) {
                offersByProductId.clear();
                for (ProductDetails productDetails : result.getProductDetailsList()) {
                    PlanOffer offer = findMonthlyBaseOffer(productDetails);
                    if (offer != null) offersByProductId.put(productDetails.getProductId(), offer);
                }
                renderPlanCards();
                if (activePlanId == null) {
                    setStatus(!billingLaunchReady
                            ? "Purchases are not enabled yet. No charge can be made."
                            : offersByProductId.isEmpty()
                                    ? "These plans are not available in Google Play for this account yet. No charge can be made."
                                    : "Choose a monthly plan. Google Play will show the final localized price before purchase.");
                }
            } else {
                setStatus("Could not load Google Play prices. Retry when your connection is available.");
            }
            updatePlanButtons();
        });
    }

    private PlanOffer findMonthlyBaseOffer(ProductDetails details) {
        List<ProductDetails.SubscriptionOfferDetails> subscriptionOffers = details.getSubscriptionOfferDetails();
        if (subscriptionOffers == null) return null;
        for (ProductDetails.SubscriptionOfferDetails offer : subscriptionOffers) {
            // Only show the ordinary base plan. Introductory pricing is not configured in this app flow.
            if (offer.getOfferId() != null) continue;
            List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
            ProductDetails.PricingPhase monthlyRecurring = null;
            for (ProductDetails.PricingPhase phase : phases) {
                if ("P1M".equals(phase.getBillingPeriod())
                        && phase.getRecurrenceMode() == ProductDetails.RecurrenceMode.INFINITE_RECURRING) {
                    monthlyRecurring = phase;
                }
            }
            if (monthlyRecurring != null) {
                return new PlanOffer(details, offer.getOfferToken(), monthlyRecurring.getFormattedPrice());
            }
        }
        return null;
    }

    private void renderPlanCards() {
        if (planCards == null) return;
        planCards.removeAllViews();
        buttonsByProductId.clear();
        for (ProviderPlanCatalog.Plan plan : ProviderPlanCatalog.PLANS) {
            PlanOffer offer = offersByProductId.get(plan.productId);
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.bottomMargin = dp(12);
            card.setLayoutParams(cardParams);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.light_gray));

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(16), dp(16), dp(16), dp(16));

            TextView title = new TextView(this);
            title.setText(plan.title);
            title.setTextColor(ContextCompat.getColor(this, R.color.black));
            title.setTextSize(19);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            content.addView(title);

            TextView benefit = new TextView(this);
            benefit.setText(plan.benefitText());
            benefit.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
            benefit.setTextSize(15);
            LinearLayout.LayoutParams benefitParams = wrapParams();
            benefitParams.topMargin = dp(6);
            content.addView(benefit, benefitParams);

            TextView price = new TextView(this);
            price.setText(offer != null
                    ? offer.localizedPrice + " / month"
                    : "Unavailable in Google Play");
            price.setTextColor(ContextCompat.getColor(this,
                    offer != null ? R.color.primary : R.color.dark_gray));
            price.setTextSize(18);
            price.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams priceParams = wrapParams();
            priceParams.topMargin = dp(12);
            content.addView(price, priceParams);

            TextView renewal = new TextView(this);
            renewal.setText("Monthly recurring subscription. Cancel in Google Play.");
            renewal.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
            renewal.setTextSize(12);
            LinearLayout.LayoutParams renewalParams = wrapParams();
            renewalParams.topMargin = dp(4);
            content.addView(renewal, renewalParams);

            Button purchaseButton = new Button(this);
            purchaseButton.setText("Continue with Google Play");
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
            buttonParams.topMargin = dp(12);
            content.addView(purchaseButton, buttonParams);
            purchaseButton.setOnClickListener(v -> startPurchase(plan, offer));
            buttonsByProductId.put(plan.productId, purchaseButton);

            card.addView(content);
            planCards.addView(card);
        }
        updatePlanButtons();
    }

    private void updatePlanButtons() {
        for (ProviderPlanCatalog.Plan plan : ProviderPlanCatalog.PLANS) {
            Button button = buttonsByProductId.get(plan.productId);
            if (button == null) continue;
            boolean active = activePlanId != null;
            boolean available = offersByProductId.containsKey(plan.productId);
            if (active && activePlanId.equals(plan.id)) {
                button.setText("Current plan");
                button.setEnabled(false);
            } else if (active) {
                button.setText("Manage current plan in Google Play");
                button.setEnabled(false);
            } else if (!billingLaunchReady) {
                button.setText("Purchases not enabled yet");
                button.setEnabled(false);
            } else if (available) {
                button.setText("Continue with Google Play");
                button.setEnabled(true);
            } else {
                button.setText(productQueryComplete ? "Not available yet" : "Loading Google Play offer…");
                button.setEnabled(false);
            }
        }
    }

    private void startPurchase(ProviderPlanCatalog.Plan plan, PlanOffer offer) {
        if (!billingLaunchReady || offer == null || activePlanId != null
                || billingClient == null || !billingClient.isReady()) {
            setStatus("Google Play is not ready. Try again in a moment.");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            setStatus("Sign in before choosing a provider plan.");
            return;
        }
        try {
            String accountId = sha256Hex(user.getUid());
            BillingFlowParams.ProductDetailsParams productParams = BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(offer.productDetails)
                    .setOfferToken(offer.offerToken)
                    .build();
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(java.util.Collections.singletonList(productParams))
                    .setObfuscatedAccountId(accountId)
                    .build();
            GoProxAnalytics.logProviderBillingStarted(this, plan.id);
            BillingResult result = billingClient.launchBillingFlow(this, flowParams);
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                setStatus("Google Play could not start the purchase. No plan was activated.");
            }
        } catch (Exception error) {
            setStatus("Google Play could not start the purchase. Try again.");
        }
    }

    private void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            setStatus("Purchase canceled. Your current plan was not changed.");
            return;
        }
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK || purchases == null) {
            setStatus("Google Play could not complete that purchase. Check your connection and try again.");
            return;
        }
        processPurchases(purchases, true);
    }

    private void queryOwnedPurchases() {
        if (billingClient == null || !billingClient.isReady()) return;
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        billingClient.queryPurchasesAsync(params, (result, purchases) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases, false);
            }
        });
    }

    private void restorePurchases() {
        if (billingClient == null || !billingClient.isReady()) {
            setStatus("Reconnecting to Google Play before checking purchases…");
            connectBilling();
            return;
        }
        setStatus("Checking purchases with Google Play…");
        queryOwnedPurchases();
    }

    private void processPurchases(List<Purchase> purchases, boolean purchaseFlowResult) {
        if (purchases == null) return;
        boolean foundPurchasedPlan = false;
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                setStatus("Payment is pending. Promotion stays locked until Google Play confirms it.");
                continue;
            }
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            for (String productId : purchase.getProducts()) {
                ProviderPlanCatalog.Plan plan = ProviderPlanCatalog.byProductId(productId);
                if (plan == null) continue;
                foundPurchasedPlan = true;
                verifyPurchase(plan, purchase.getPurchaseToken(), purchaseFlowResult);
                break;
            }
        }
        if (!foundPurchasedPlan && (purchases == null || purchases.isEmpty())) {
            setStatus("No GoProx subscription was found for this Google Play account.");
        }
    }

    private void verifyPurchase(ProviderPlanCatalog.Plan plan, String purchaseToken,
                                boolean purchaseFlowResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", plan.productId);
        payload.put("purchaseToken", purchaseToken);
        FirebaseFunctions.getInstance("europe-west1")
                .getHttpsCallable("verifyGoProxSubscription")
                .call(payload)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setStatus("Google Play completed the purchase, but GoProx could not finish verification. Restore purchases or retry; benefits unlock only after server verification.");
                        refreshEntitlement();
                        return;
                    }
                    Object raw = task.getResult() == null ? null : task.getResult().getData();
                    if (!(raw instanceof Map)) {
                        setStatus("The purchase response was incomplete. Retry verification from Restore purchases.");
                        return;
                    }
                    Object status = ((Map<?, ?>) raw).get("status");
                    if ("pending".equals(status)) {
                        setStatus("Payment is pending. Promotion stays locked until Google Play confirms it.");
                    } else if ("active".equals(status)) {
                        activePlanId = plan.id;
                        Object expiry = ((Map<?, ?>) raw).get("expiryMillis");
                        activeExpiryMillis = expiry instanceof Number ? ((Number) expiry).longValue() : 0L;
                        Object usedSlots = ((Map<?, ?>) raw).get("activePromotionCount");
                        activePromotionCount = usedSlots instanceof Number
                                ? ((Number) usedSlots).intValue() : activePromotionCount;
                        if (purchaseFlowResult) GoProxAnalytics.logProviderPlanActivated(this, plan.id);
                        setStatus(currentPlanStatus(plan));
                        updatePlanButtons();
                        refreshEntitlement();
                        promoteRequestedServiceIfNeeded();
                    } else {
                        setStatus("Google Play did not confirm an active plan. No promotion benefit was unlocked.");
                        refreshEntitlement();
                    }
                });
    }

    private void refreshEntitlement() {
        FirebaseFunctions.getInstance("europe-west1")
                .getHttpsCallable("getGoProxEntitlement")
                .call()
                .addOnSuccessListener(result -> {
                    Object raw = result.getData();
                    if (!(raw instanceof Map)) return;
                    Map<?, ?> data = (Map<?, ?>) raw;
                    if (!Boolean.TRUE.equals(data.get("active"))) {
                        activePlanId = null;
                        activeExpiryMillis = 0L;
                    activePromotionCount = 0;
                        updatePlanButtons();
                        return;
                    }
                    Object planId = data.get("planId");
                    Object expiry = data.get("expiryMillis");
                    Object usedSlots = data.get("activePromotionCount");
                    activePlanId = planId instanceof String ? (String) planId : null;
                    activeExpiryMillis = expiry instanceof Number ? ((Number) expiry).longValue() : 0L;
                    activePromotionCount = usedSlots instanceof Number ? ((Number) usedSlots).intValue() : 0;
                    ProviderPlanCatalog.Plan plan = findPlan(activePlanId);
                    if (plan != null) {
                        setStatus(currentPlanStatus(plan)
                                + (Boolean.TRUE.equals(data.get("autoRenewEnabled"))
                                ? " Renews unless canceled in Google Play."
                                : " Manage renewal in Google Play."));
                        promoteRequestedServiceIfNeeded();
                    }
                    updatePlanButtons();
                })
                .addOnFailureListener(error -> {
                    if (activePlanId == null) {
                        setStatus("Could not check your plan right now. You can retry or restore purchases.");
                    }
                });
    }

    private void promoteRequestedServiceIfNeeded() {
        if (promotionRequested || activePlanId == null || serviceIdToPromote == null
                || serviceIdToPromote.trim().isEmpty()) return;
        promotionRequested = true;
        setStatus("Your plan is active. Activating promotion for your listing…");
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceId", serviceIdToPromote);
        FirebaseFunctions.getInstance("europe-west1")
                .getHttpsCallable("useProviderPromotion")
                .call(payload)
                .addOnSuccessListener(result -> {
                    Object raw = result.getData();
                    boolean promoted = raw instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) raw).get("promoted"));
                    if (promoted) {
                        GoProxAnalytics.logPromotionActivated(this);
                        setStatus("Your listing is now marked Sponsored until " + formatDate(activeExpiryMillis)
                                + ". It remains subject to search and location filters.");
                    }
                })
                .addOnFailureListener(error -> {
                    promotionRequested = false;
                    setStatus("Your subscription is active, but this listing could not be promoted. "
                            + "Check that you own the listing and have an available promotion slot.");
                });
    }

    private void openPlaySubscriptions() {
        Uri uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=" + getPackageName());
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Google Play subscription settings are unavailable.", Toast.LENGTH_LONG).show();
        }
    }

    private String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte item : digest) hex.append(String.format(Locale.ROOT, "%02x", item));
        return hex.toString();
    }

    private ProviderPlanCatalog.Plan findPlan(String id) {
        if (id == null) return null;
        for (ProviderPlanCatalog.Plan plan : ProviderPlanCatalog.PLANS) {
            if (plan.id.equals(id)) return plan;
        }
        return null;
    }

    private String currentPlanStatus(ProviderPlanCatalog.Plan plan) {
        return plan.title + " · " + activePromotionCount + "/" + plan.promotionSlots
                + " promotion slots used · active until " + formatDate(activeExpiryMillis) + ".";
    }

    private String formatDate(long millis) {
        if (millis <= 0) return "your next billing date";
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private void setStatus(String message) {
        if (statusText != null) statusText.setText(message);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    @Override
    protected void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        refreshEntitlement();
        queryOwnedPurchases();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static final class PlanOffer {
        final ProductDetails productDetails;
        final String offerToken;
        final String localizedPrice;

        PlanOffer(ProductDetails productDetails, String offerToken, String localizedPrice) {
            this.productDetails = productDetails;
            this.offerToken = offerToken;
            this.localizedPrice = localizedPrice;
        }
    }
}
