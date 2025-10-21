package com.yanapure.app.sms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "twilio.account-sid=", // Empty to disable Twilio
    "twilio.auth-token=",
    "twilio.phone-number="
})
public class SmsConfigTest {
    
    @Autowired
    private SmsProvider smsProvider;
    
    @Test
    void testInMemoryProviderIsUsedWhenTwilioNotConfigured() {
        assertNotNull(smsProvider);
        assertEquals("InMemorySmsProvider", smsProvider.getProviderName());
        assertTrue(smsProvider instanceof InMemorySmsProvider);
    }
    
    @Test
    void testSmsProviderCanSendMessage() {
        boolean result = smsProvider.sendSms("+14155552671", "Test message");
        assertTrue(result);
    }
}
