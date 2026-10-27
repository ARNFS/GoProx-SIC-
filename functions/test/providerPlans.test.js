const test = require("node:test");
const assert = require("node:assert/strict");
const { getProviderPlan, hasActiveSubscriptionEntitlement } = require("../lib/providerPlans.js");

test("maps each Play product to the matching promotion capacity", () => {
    assert.deepEqual(
        ["lux", "pro", "vip", "premium"].map((id, index) => {
            const productId = [
                "goprox_lux_monthly",
                "goprox_pro_monthly",
                "goprox_vip_monthly",
                "goprox_premium_monthly",
            ][index];
            const plan = getProviderPlan(productId);
            return [plan.id, plan.concurrentPromotionSlots];
        }),
        [["lux", 1], ["pro", 3], ["vip", 8], ["premium", 20]]
    );
    assert.equal(getProviderPlan("unknown"), undefined);
});

test("only active, grace-period, or not-yet-expired canceled subscriptions confer entitlement", () => {
    const now = 1_800_000_000_000;
    const future = new Date(now + 60_000).toISOString();
    const past = new Date(now - 60_000).toISOString();

    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_ACTIVE", future, now), true);
    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_IN_GRACE_PERIOD", future, now), true);
    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_CANCELED", future, now), true);
    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_CANCELED", past, now), false);
    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_PENDING", future, now), false);
    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_ON_HOLD", future, now), false);
    assert.equal(hasActiveSubscriptionEntitlement("SUBSCRIPTION_STATE_ACTIVE", "invalid", now), false);
});
