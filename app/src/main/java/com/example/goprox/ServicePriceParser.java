
package com.example.goprox;

/**
 * Converts Firestore price values into strings.
 *
 * Supports numeric prices and legacy string prices.
 * This class does not depend on Firebase, so it can be
 * tested with local unit tests.
 */
public final class ServicePriceParser {

    private ServicePriceParser() {
        // Utility class; instances are not needed.
    }

    public static String toPriceString(Object rawPrice) {
        if (rawPrice == null) {
            return "0";
        }

        if (rawPrice instanceof Number) {
            double value = ((Number) rawPrice).doubleValue();

            if (!Double.isFinite(value)) {
                return "0";
            }

            if (value == Math.rint(value)) {
                return String.valueOf((long) value);
            }

            return String.valueOf(value);
        }

        if (rawPrice instanceof String) {
            String value = ((String) rawPrice).trim();
            return value.isEmpty() ? "0" : value;
        }

        return "0";
    }
}
