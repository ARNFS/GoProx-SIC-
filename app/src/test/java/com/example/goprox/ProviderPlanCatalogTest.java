package com.example.goprox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ProviderPlanCatalogTest {
    @Test
    public void exposesMatchingProductsAndConcurrentPromotionCapacity() {
        assertEquals("goprox_lux_monthly", ProviderPlanCatalog.PLANS.get(0).productId);
        assertEquals(1, ProviderPlanCatalog.PLANS.get(0).promotionSlots);
        assertEquals("goprox_pro_monthly", ProviderPlanCatalog.PLANS.get(1).productId);
        assertEquals(3, ProviderPlanCatalog.PLANS.get(1).promotionSlots);
        assertEquals("goprox_vip_monthly", ProviderPlanCatalog.PLANS.get(2).productId);
        assertEquals(8, ProviderPlanCatalog.PLANS.get(2).promotionSlots);
        assertEquals("goprox_premium_monthly", ProviderPlanCatalog.PLANS.get(3).productId);
        assertEquals(20, ProviderPlanCatalog.PLANS.get(3).promotionSlots);
    }

    @Test
    public void resolvesOnlyKnownProductIds() {
        assertEquals("vip", ProviderPlanCatalog.byProductId("goprox_vip_monthly").id);
        assertNull(ProviderPlanCatalog.byProductId("unconfigured_product"));
    }
}
