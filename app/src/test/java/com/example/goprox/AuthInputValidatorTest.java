package com.example.goprox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthInputValidatorTest {
    @Test
    public void normalizesEmailWithoutChangingPasswordContents() {
        assertEquals("person@example.com", AuthInputValidator.normalizeEmail("  Person@Example.com "));
        assertTrue(AuthInputValidator.isValidEmail("person@example.com"));
        assertFalse(AuthInputValidator.isValidEmail("not-an-email"));
        assertTrue(AuthInputValidator.isValidPassword(" 12345 "));
        assertFalse(AuthInputValidator.isValidPassword("12345"));
    }

    @Test
    public void requiresExactPasswordConfirmation() {
        assertTrue(AuthInputValidator.passwordsMatch("secret1", "secret1"));
        assertFalse(AuthInputValidator.passwordsMatch("secret1", "secret1 "));
        assertFalse(AuthInputValidator.passwordsMatch(null, "secret1"));
    }
}
