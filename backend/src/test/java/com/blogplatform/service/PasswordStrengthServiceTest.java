package com.blogplatform.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PasswordStrengthServiceTest {

    private final PasswordStrengthService service = new PasswordStrengthService();

    @Test
    void shouldReturnStrongWhenPasswordComplexEnough() {
        var result = service.evaluate("StrongPass123!");
        Assertions.assertEquals("STRONG", result.getLevel());
        Assertions.assertTrue(service.isStrongEnough("StrongPass123!"));
    }

    @Test
    void shouldReturnWeakWhenPasswordTooSimple() {
        var result = service.evaluate("12345678");
        Assertions.assertEquals("WEAK", result.getLevel());
        Assertions.assertFalse(service.isStrongEnough("12345678"));
    }
}
