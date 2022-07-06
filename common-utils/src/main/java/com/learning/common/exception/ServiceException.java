package com.learning.common.exception;

import io.grpc.Status;

public class ServiceException extends Exception {
  private Status status;

  public ServiceException(Status status) {
    this.status = status;
  }

  public ServiceException(Status status, String message, Object... args) {
    super(String.format(message != null ? message : "", args));
    this.status = status.withDescription(String.format(message, args));
  }

  public ServiceException(Status status, Throwable ex) {
    this.status = status.withCause(ex);
    this.initCause(ex);
  }

  /**
   * Constructor for {@link ServiceException}.
   *
   * @param status the given exception status
   * @param message the given exception message
   * @param cause the given exception cause
   */
  public ServiceException(Status status, String message, Throwable cause) {
    this(status, message);
    this.initCause(cause);
    this.status = status.withCause(cause);
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
