package com.example.goprox;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/** Privacy-minimal product funnel events; never attach emails, UIDs, query text, or purchase tokens. */
final class GoProxAnalytics {
    private GoProxAnalytics() { }

    static void logHomeOpened(Context context) {
        log(context, "home_opened");
    }

    static void logServiceDetailOpened(Context context) {
        log(context, "service_detail_opened");
    }

    static void logProviderListingCreated(Context context) {
        log(context, "provider_listing_created");
    }

    static void logServiceRequestSubmitted(Context context) {
        log(context, "service_request_submitted");
    }

    static void logProviderPlansOpened(Context context) {
        log(context, "provider_plans_opened", null);
    }

    static void logProviderBillingStarted(Context context, String planId) {
        logPlanEvent(context, "provider_billing_started", planId);
    }

    static void logProviderPlanActivated(Context context, String planId) {
        logPlanEvent(context, "provider_plan_activated", planId);
    }

    static void logPromotionActivated(Context context) {
        log(context, "provider_promotion_activated", null);
    }

    static void logPromotionRemoved(Context context) {
        log(context, "provider_promotion_removed", null);
    }

    private static void logPlanEvent(Context context, String eventName, String planId) {
        Bundle params = new Bundle();
        if (planId != null) params.putString("plan_id", planId);
        log(context, eventName, params);
    }

    private static void log(Context context, String eventName) {
        if (context == null) return;
        try {
            FirebaseAnalytics.getInstance(context).logEvent(eventName, null);
        } catch (RuntimeException ignored) {
            // Analytics must never block a marketplace action.
        }
    }

    private static void log(Context context, String eventName, Bundle params) {
        if (context == null) return;
        try {
            FirebaseAnalytics.getInstance(context).logEvent(eventName, params);
        } catch (RuntimeException ignored) {
            // Analytics must never block a marketplace or billing action.
        }
    }
}
