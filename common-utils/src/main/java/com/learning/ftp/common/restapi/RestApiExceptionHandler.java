package com.learning.ftp.common.restapi;

import com.learning.ftp.common.exception.FtpServiceException;
import io.grpc.Status;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler {
  @ExceptionHandler(Throwable.class)
  @ResponseBody
  public ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
    String message = ex.getMessage();
    if (isEmpty(message) && ex.getCause() != null) {
      message = ex.getCause().getMessage();
    }
    if (isBusinessException(ex)) {
      logger.warn(message);
    } else {
      logger.error("Uncatched error: " + message, ex);
    }
    return createResponseEntity(request, ex);
  }

  private boolean isBusinessException(Throwable e) {
    return Optional.ofNullable(e)
        .filter(FtpServiceException.class::isInstance)
        .map(FtpServiceException.class::cast)
        .map(FtpServiceException::getHttpStatus)
        .filter(HttpStatus::is4xxClientError)
        .isPresent();
  }

  private ResponseEntity<?> createResponseEntity(HttpServletRequest request, Throwable ex) {
    HttpStatus httpStatus =
        ex instanceof FtpServiceException
            ? ((FtpServiceException) ex).getHttpStatus()
            : HttpStatus.INTERNAL_SERVER_ERROR;
    return new ResponseEntity<>(createRestApiException(request, ex), httpStatus);
  }

  private RestApiException createRestApiException(HttpServletRequest request, Throwable ex) {
    if (ex instanceof FtpServiceException) {
      return new RestApiException(
          Status.UNKNOWN.getCode().value(),
          ((FtpServiceException) ex).getMessageCode(),
          ex.getMessage());
    }
    return new RestApiException(Status.UNKNOWN.getCode().value(), "0000000", ex.getMessage());
  }

  private static class RestApiException {
    private int status;
    private String messageCode;
    private String messageError;

    public RestApiException(int status, String messageCode, String messageError) {
      this.status = status;
      this.messageCode = messageCode;
      this.messageError = messageError;
    }

    public int getStatus() {
      return status;
    }

    public String getMessageCode() {
      return messageCode;
    }

    public String getMessageError() {
      return messageError;
    }
  }
}
