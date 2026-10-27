package com.example.goprox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Applies every search against the full source list so edits can broaden results again. */
final class ServiceSearchFilter {
    private ServiceSearchFilter() { }

    static List<Service> filter(List<Service> source, String query) {
        List<Service> results = new ArrayList<>();
        if (source == null) return results;

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Service service : source) {
            if (service == null) continue;
            if (normalizedQuery.isEmpty()
                    || contains(service.getName(), normalizedQuery)
                    || contains(service.getProfession(), normalizedQuery)) {
                results.add(service);
            }
        }
        return results;
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
