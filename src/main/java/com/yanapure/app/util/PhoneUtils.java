package com.yanapure.app.util;

public final class PhoneUtils {
    private PhoneUtils() {
    }

    // + followed by 9-15 digits => total length 10-16
    private static final String E164_REGEX = "^\\+[0-9]{9,15}$";

    public static boolean isValidE164(String s) {
        return s != null && s.matches(E164_REGEX);
    }

    public static String normalizeToE164(String raw) {
        if (raw == null)
            throw new PhoneValidationException("Phone required");
        String cleaned = raw.replaceAll("[\\s\\-().]", "");
        if (!isValidE164(cleaned)) {
            throw new PhoneValidationException("Phone must be E.164 (e.g., +14155552671)");
        }
        return cleaned;
    }

    public static void validatePhone(String raw) {
        normalizeToE164(raw);
    }

    /**
     * Mask phone number for logging (e.g., +14155552671 -> +1*******71)
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return phone.substring(0, 2) + "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 2);
    }
}
