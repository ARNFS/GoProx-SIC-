package com.example.goprox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Product IDs must stay aligned with functions/src/providerPlans.ts and Play Console. */
final class ProviderPlanCatalog {
    static final class Plan {
        final String id;
        final String productId;
        final String title;
        final int promotionSlots;

        Plan(String id, String productId, String title, int promotionSlots) {
            this.id = id;
            this.productId = productId;
            this.title = title;
            this.promotionSlots = promotionSlots;
        }

        String benefitText() {
            return promotionSlots == 1
                    ? "Promote 1 of your listings at a time"
                    : "Promote up to " + promotionSlots + " of your listings at a time";
        }
    }

    static final List<Plan> PLANS = Collections.unmodifiableList(Arrays.asList(
            new Plan("lux", "goprox_lux_monthly", "GoProx Lux", 1),
            new Plan("pro", "goprox_pro_monthly", "GoProx Pro", 3),
            new Plan("vip", "goprox_vip_monthly", "GoProx VIP", 8),
            new Plan("premium", "goprox_premium_monthly", "GoProx Premium", 20)
    ));

    private ProviderPlanCatalog() { }

    static Plan byProductId(String productId) {
        if (productId == null) return null;
        for (Plan plan : PLANS) {
            if (plan.productId.equals(productId)) return plan;
        }
        return null;
    }
}
