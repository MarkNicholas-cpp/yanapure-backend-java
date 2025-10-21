package com.yanapure.app.sms;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SmsConfigTest {

  @Test
  void testInMemoryProviderDirectly() {
    InMemorySmsProvider provider = new InMemorySmsProvider();

    assertNotNull(provider);
    assertEquals("InMemorySmsProvider", provider.getProviderName());
    assertTrue(provider instanceof SmsProvider);
  }

  @Test
  void testInMemoryProviderCanSendMessage() {
    InMemorySmsProvider provider = new InMemorySmsProvider();
    boolean result = provider.sendSms("+14155552671", "Test message");

    assertTrue(result);
    assertEquals(1, provider.getMessageCount("+14155552671"));
    assertEquals("Test message", provider.getLastMessage("+14155552671"));
  }
}
