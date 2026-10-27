export type ProviderPlanId = "lux" | "pro" | "vip" | "premium";

export interface ProviderPlan {
    id: ProviderPlanId;
    productId: string;
    concurrentPromotionSlots: number;
}

export const PROVIDER_PLANS: readonly ProviderPlan[] = [
    { id: "lux", productId: "goprox_lux_monthly", concurrentPromotionSlots: 1 },
    { id: "pro", productId: "goprox_pro_monthly", concurrentPromotionSlots: 3 },
    { id: "vip", productId: "goprox_vip_monthly", concurrentPromotionSlots: 8 },
    { id: "premium", productId: "goprox_premium_monthly", concurrentPromotionSlots: 20 },
];

export function getProviderPlan(
    productId: unknown
): ProviderPlan | undefined {
    if (typeof productId !== "string") return undefined;
    return PROVIDER_PLANS.find(plan => plan.productId === productId);
}

export function hasActiveSubscriptionEntitlement(
    subscriptionState: unknown,
    expiryTimeMillis: unknown,
    nowMillis = Date.now()
): boolean {
    if (
        subscriptionState !== "SUBSCRIPTION_STATE_ACTIVE" &&
        subscriptionState !== "SUBSCRIPTION_STATE_IN_GRACE_PERIOD" &&
        subscriptionState !== "SUBSCRIPTION_STATE_CANCELED"
    ) {
        return false;
    }

    const expiryMillis = typeof expiryTimeMillis === "number"
        ? expiryTimeMillis
        : typeof expiryTimeMillis === "string"
            ? Date.parse(expiryTimeMillis)
            : Number.NaN;

    return Number.isFinite(expiryMillis) && expiryMillis > nowMillis;
}
