package com.yanapure.app.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** SMS configuration that autowires InMemorySmsProvider if Twilio is not configured */
@Configuration
public class SmsConfig {

  /** Twilio SMS provider - only created when Twilio credentials are configured */
  @Bean
  @ConditionalOnProperty(name = "twilio.account-sid", matchIfMissing = false)
  public SmsProvider twilioSmsProvider(
      @Value("${twilio.account-sid}") String accountSid,
      @Value("${twilio.auth-token}") String authToken,
      @Value("${twilio.phone-number}") String phoneNumber) {
    return new TwilioSmsProvider(accountSid, authToken, phoneNumber);
  }

  /** Primary SMS provider - InMemorySmsProvider as fallback */
  @Bean
  @Primary
  public SmsProvider smsProvider(InMemorySmsProvider inMemoryProvider) {
    return inMemoryProvider;
  }
}
