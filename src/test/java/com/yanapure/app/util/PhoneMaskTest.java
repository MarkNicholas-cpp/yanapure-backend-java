package com.yanapure.app.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PhoneMaskTest {

  @Test
  void testMasking() {
    assertEquals("+91******88", PhoneMask.mask("+918008297288")); // cc=91 -> 6 stars, last2=88
    assertEquals("+1*******71", PhoneMask.mask("+14155552671")); // cc=1 -> 7 stars, last2=71
    assertEquals("+44******23", PhoneMask.mask("+447700900123")); // cc=44 -> 6 stars, last2=23
  }

  @Test
  void testMaskWithCustomVisibleDigits() {
    assertEquals("+91******7288", PhoneMask.mask("+918008297288", 4));
    assertEquals("+1******671", PhoneMask.mask("+14155552671", 3));
    assertEquals("+44******0123", PhoneMask.mask("+447700900123", 4));
  }

  @Test
  void testMaskCompletely() {
    assertEquals("+**********", PhoneMask.maskCompletely("+918008297288"));
  }

  @Test
  void testMaskInvalidNumbers() {
    assertEquals("***", PhoneMask.mask("invalid"));
    assertEquals("***", PhoneMask.mask(null));
    assertEquals("***", PhoneMask.mask("+12abc"));
  }

  @Test
  void testIsMasked() {
    assertTrue(PhoneMask.isMasked("+91******88"));
    assertFalse(PhoneMask.isMasked("+918008297288"));
  }

  @Test
  void testMaskChar() {
    assertEquals('*', PhoneMask.getMaskChar());
  }
}
