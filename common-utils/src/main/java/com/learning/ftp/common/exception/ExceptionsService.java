package com.learning.ftp.common.exception;

import org.springframework.http.HttpStatus;

import java.util.function.Supplier;

public interface ExceptionsService {
  void throwEx(String messageId, HttpStatus httpStatus, Object... args);

  Supplier<FtpServiceException> getError(String messageId, HttpStatus httpStatus, Object... args);
}
