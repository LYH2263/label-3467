package com.blogplatform.util;

import java.text.Normalizer;

public final class SlugUtil {

    private SlugUtil() {
    }

    public static String toSlug(String value) {
        if (value == null || value.isBlank()) {
            return "post-" + System.currentTimeMillis();
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (slug.isBlank()) {
            return "post-" + System.currentTimeMillis();
        }
        return slug;
    }
}
