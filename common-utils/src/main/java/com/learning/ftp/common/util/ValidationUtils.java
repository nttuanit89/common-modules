package com.learning.ftp.common.util;

import com.learning.ftp.common.exception.ExceptionsService;
import java.util.Objects;
import static java.util.Optional.ofNullable;
import org.springframework.context.annotation.Lazy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Lazy
@Service
public class ValidationUtils {
  private final ExceptionsService exceptions;

  ValidationUtils(ExceptionsService exceptions) {
    this.exceptions = exceptions;
  }

  public void requireField(String messageId, Object value, Object... args) {
    ofNullable(value)
        .filter(this::nonEmpty)
        .orElseThrow(exceptions.getError(messageId, BAD_REQUEST, args));
  }

  private boolean nonEmpty(Object o) {
    if (Objects.isNull(o)) {
      return false;
    }

    if (o instanceof String) {
      return StringUtils.hasText(o.toString());
    }

    if (o instanceof Number) {
      return ((Number) o).longValue() != 0L;
    }

    return true;
  }
}
