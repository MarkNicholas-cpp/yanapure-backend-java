package com.yanapure.app.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Twilio SMS provider for production SMS sending Only instantiated when Twilio credentials are
 * configured
 */
public class TwilioSmsProvider implements SmsProvider {

  private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

  private final String accountSid;
  private final String authToken;
  private final String fromPhoneNumber;

  public TwilioSmsProvider(String accountSid, String authToken, String fromPhoneNumber) {
    this.accountSid = accountSid;
    this.authToken = authToken;
    this.fromPhoneNumber = fromPhoneNumber;
    Twilio.init(accountSid, authToken);
    log.info("Twilio SMS provider initialized with account: {}", maskAccountSid(accountSid));
  }

  @Override
  public boolean sendSms(String phoneNumber, String message) {
    try {
      Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(fromPhoneNumber), message)
          .create();

      log.info("SMS sent successfully to: {}", phoneNumber);
      return true;

    } catch (Exception e) {
      log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
      return false;
    }
  }

  @Override
  public String getProviderName() {
    return "TwilioSmsProvider";
  }

  /** Mask account SID for logging (show first 4 and last 4 characters) */
  private String maskAccountSid(String accountSid) {
    if (accountSid == null || accountSid.length() < 8) {
      return "***";
    }
    return accountSid.substring(0, 4) + "****" + accountSid.substring(accountSid.length() - 4);
  }
}
