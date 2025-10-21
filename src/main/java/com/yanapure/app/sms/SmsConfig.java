package com.yanapure.app.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * SMS configuration that autowires InMemorySmsProvider if Twilio is not configured
 */
@Configuration
public class SmsConfig {
    
    /**
     * Primary SMS provider - will be InMemorySmsProvider if Twilio is not configured
     * TwilioSmsProvider will be instantiated only when twilio.account-sid is set
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "twilioSmsProvider")
    public SmsProvider smsProvider(InMemorySmsProvider inMemoryProvider) {
        return inMemoryProvider;
    }
}
