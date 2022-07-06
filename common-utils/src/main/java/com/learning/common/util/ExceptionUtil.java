package com.learning.common.util;

import com.learning.common.exception.FtpServiceException;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

public class ExceptionUtil {
  public static void throwException(String messageCode, String... messageError) {
    if (messageError.length < 1) {
      throw new FtpServiceException(messageCode);
    }
    throw new FtpServiceException(messageCode, messageError[0]);
  }

  public static void throwException(String messageCode, HttpStatus status, String... messageError) {
    if (messageError.length < 1) {
      throw new FtpServiceException(messageCode, status);
    }
    throw new FtpServiceException(messageCode, messageError[0], status);
  }

  public static Supplier<FtpServiceException> error(
      String messageCode, HttpStatus status, String... messageError) {
    if (messageError.length < 1) {
      return () -> new FtpServiceException(messageCode, status);
    }
    return () -> new FtpServiceException(messageCode, messageError[0], status);
  }
}
