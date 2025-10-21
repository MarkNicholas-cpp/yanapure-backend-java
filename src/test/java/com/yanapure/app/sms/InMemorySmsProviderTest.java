package com.yanapure.app.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Queue;

public class InMemorySmsProviderTest {

    private InMemorySmsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InMemorySmsProvider();
    }

    @Test
    void testSendSmsSuccess() {
        String phoneNumber = "+14155552671";
        String message = "Your OTP code is: 123456";

        boolean result = provider.sendSms(phoneNumber, message);

        assertTrue(result);
        assertEquals(1, provider.getMessageCount(phoneNumber));
        assertEquals(message, provider.getLastMessage(phoneNumber));
    }

    @Test
    void testSendSmsWithNullPhone() {
        boolean result = provider.sendSms(null, "test message");
        assertFalse(result);
    }

    @Test
    void testSendSmsWithNullMessage() {
        boolean result = provider.sendSms("+14155552671", null);
        assertFalse(result);
    }

    @Test
    void testMultipleMessages() {
        String phoneNumber = "+14155552671";
        String message1 = "First message";
        String message2 = "Second message";

        provider.sendSms(phoneNumber, message1);
        provider.sendSms(phoneNumber, message2);

        assertEquals(2, provider.getMessageCount(phoneNumber));
        Queue<String> allMessages = provider.getAllMessages(phoneNumber);
        assertTrue(allMessages.contains(message1));
        assertTrue(allMessages.contains(message2));
    }

    @Test
    void testGetLastMessage() {
        String phoneNumber = "+14155552671";
        String message1 = "First message";
        String message2 = "Last message";

        provider.sendSms(phoneNumber, message1);
        provider.sendSms(phoneNumber, message2);

        assertEquals(message2, provider.getLastMessage(phoneNumber));
    }

    @Test
    void testGetLastMessageForNonExistentPhone() {
        String result = provider.getLastMessage("+14155552671");
        assertNull(result);
    }

    @Test
    void testClearMessages() {
        String phoneNumber = "+14155552671";
        provider.sendSms(phoneNumber, "test message");

        assertEquals(1, provider.getMessageCount(phoneNumber));

        provider.clearMessages();

        assertEquals(0, provider.getMessageCount(phoneNumber));
        assertNull(provider.getLastMessage(phoneNumber));
    }

    @Test
    void testGetProviderName() {
        assertEquals("InMemorySmsProvider", provider.getProviderName());
    }
}
