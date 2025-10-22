package com.yanapure.app.util;

import com.yanapure.app.common.ApiException;

public class PhoneValidationException extends ApiException {
  public PhoneValidationException(String message) {
    super("PHONE_INVALID", message);
  }
}
