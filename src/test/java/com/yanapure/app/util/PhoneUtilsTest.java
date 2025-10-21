package com.yanapure.app.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PhoneUtilsTest {

  @Test
  void validExamples() {
    assertTrue(PhoneUtils.isValidE164("+14155552671"));
    assertTrue(PhoneUtils.isValidE164("+918008297288"));
    assertTrue(PhoneUtils.isValidE164("+447700900123"));
  }

  @Test
  void normalization() {
    assertEquals("+14155552671", PhoneUtils.normalizeToE164("+1 (415) 555-2671"));
  }

  @Test
  void invalidExamples() {
    assertFalse(PhoneUtils.isValidE164("14155552671")); // no +
    assertFalse(PhoneUtils.isValidE164("+12abc345"));
  }
}
