package com.yanapure.app.util;

/**
 * Masking rules:
 * - mask(e164): "+<cc><fixed-stars><last2>"
 *   * if cc = "1" => 7 stars (e.g., +1*******71)
 *   * else        => 6 stars (e.g., +91******88)
 * - mask(e164, visibleDigits): "+<cc>******<tailN>"
 * - maskCompletely(): "+**********" (10 stars)
 * - invalid input => "***"
 */
public final class PhoneMask {
    private PhoneMask(){}

    public static String mask(String e164) {
        if (!PhoneUtils.isValidE164(e164)) return "***";
        String cc = extractCc(e164);
        if (cc == null) return "***";
        String local = e164.substring(1 + cc.length());
        if (local.length() < 2) return maskCompletely(e164);

        String tail2 = local.substring(local.length() - 2);
        int stars = (cc.length() == 1 ? 7 : 6);
        return "+" + cc + "*".repeat(stars) + tail2;
    }

    public static String mask(String e164, int visibleDigits) {
        if (!PhoneUtils.isValidE164(e164)) return "***";
        String cc = extractCc(e164);
        if (cc == null) return "***";
        if (visibleDigits < 0) visibleDigits = 0;
        String local = e164.substring(1 + cc.length());
        if (local.length() < visibleDigits) return maskCompletely(e164);
        String tail = local.substring(local.length() - visibleDigits);
        return "+" + cc + "******" + tail;
    }

    public static String maskCompletely(String ignored) {
        return "+**********";
    }

    public static boolean isMasked(String masked) {
        return masked != null && masked.indexOf('*') >= 0;
    }

    public static char getMaskChar() {
        return '*';
    }

    private static String extractCc(String e164) {
        if (e164 == null || e164.length() < 2 || e164.charAt(0) != '+') return null;
        char d1 = e164.charAt(1);
        if (!Character.isDigit(d1)) return null;
        if (d1 == '1') return "1";
        if (e164.length() >= 3 && Character.isDigit(e164.charAt(2))) {
            return "" + d1 + e164.charAt(2);
        }
        return null;
    }
}
