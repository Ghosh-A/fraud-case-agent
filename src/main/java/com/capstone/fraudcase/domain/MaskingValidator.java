package com.capstone.fraudcase.domain;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Guards the synthetic/masked-data boundary: references must be tokenised or
 * masked and must never contain a raw payment card or account number.
 */
@Component
public class MaskingValidator {

    // 12+ consecutive digits (optionally separated) look like a raw PAN/account number.
    private static final Pattern PAN_LIKE = Pattern.compile("(?:\\d[ -]?){12,19}");
    // Allowed: letters/digits plus masking bullets, dots and dashes.
    private static final Pattern REF_TOKEN = Pattern.compile("^[A-Za-z0-9 .•*\\-_]{4,64}$");
    private static final Pattern MARKUP_OR_SCRIPT = Pattern.compile(
            "<\\s*/?\\s*(script|iframe|object|embed|form|svg)[^>]*>|<\\s*[a-zA-Z]+[^>]*>|javascript:",
            Pattern.CASE_INSENSITIVE);

    public boolean isMaskedReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return REF_TOKEN.matcher(value).matches() && !containsUnmaskedIdentifier(value);
    }

    public boolean containsUnmaskedIdentifier(String value) {
        return value != null && PAN_LIKE.matcher(value.replaceAll("[•*xX]", "")).find();
    }

    public boolean containsMarkup(String text) {
        return text != null && MARKUP_OR_SCRIPT.matcher(text).find();
    }
}
