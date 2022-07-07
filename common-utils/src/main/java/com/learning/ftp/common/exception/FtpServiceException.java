package com.learning.ftp.common.exception;

import org.springframework.http.HttpStatus;

public class FtpServiceException extends RuntimeException {
  private String messageCode;
  private HttpStatus httpStatus;

  public FtpServiceException(String messageCode) {
    this(messageCode, null, null, null);
  }

  public FtpServiceException(String messageCode, Throwable cause) {
    this(messageCode, null, null, cause);
  }

  public FtpServiceException(String messageCode, String messageError) {
    this(messageCode, messageError, null, null);
  }

  public FtpServiceException(String messageCode, HttpStatus httpStatus) {
    this(messageCode, null, httpStatus, null);
  }

  public FtpServiceException(String messageCode, String messageError, Throwable cause) {
    this(messageCode, messageError, null, cause);
  }

  public FtpServiceException(String messageCode, HttpStatus httpStatus, Throwable cause) {
    this(messageCode, null, httpStatus, cause);
  }

  public FtpServiceException(String messageCode, String messageError, HttpStatus httpStatus) {
    this(messageCode, messageError, httpStatus, null);
  }

  public FtpServiceException(
      String messageCode, String messageError, HttpStatus httpStatus, Throwable cause) {
    super(messageError == null ? "" : messageError, cause);
    this.messageCode = messageCode;
    this.httpStatus = httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus;
  }

  public String getMessageCode() {
    return messageCode;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
