package com.yanapure.app.sms;

/** SMS provider abstraction for sending OTP codes */
public interface SmsProvider {

  /**
   * Send SMS message to the specified phone number
   *
   * @param phoneNumber E.164 formatted phone number (e.g., +14155552671)
   * @param message SMS message content
   * @return true if message was sent successfully, false otherwise
   */
  boolean sendSms(String phoneNumber, String message);

  /**
   * Get provider name for logging/debugging
   *
   * @return provider name
   */
  String getProviderName();
}
