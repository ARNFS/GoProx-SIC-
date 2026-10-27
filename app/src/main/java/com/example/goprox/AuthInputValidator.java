package com.example.goprox;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared validation for email/password sign-in and account creation. */
final class AuthInputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private AuthInputValidator() { }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isValidEmail(String email) {
        String normalized = normalizeEmail(email);
        return !normalized.isEmpty() && EMAIL_PATTERN.matcher(normalized).matches();
    }

    static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    static boolean passwordsMatch(String password, String confirmation) {
        return password != null && password.equals(confirmation);
    }
}
