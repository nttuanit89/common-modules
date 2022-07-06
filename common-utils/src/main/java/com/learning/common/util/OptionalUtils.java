package com.learning.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.StringUtils.isBlank;

public interface OptionalUtils {

  @SuppressWarnings("rawtypes")
  static boolean isNullOrEmpty(Object value) {
    return Objects.isNull(value)
        || (value instanceof String && isBlank(value.toString()))
        || (value instanceof Collection && ((Collection) value).isEmpty())
        || (value instanceof Map && ((Map) value).isEmpty());
  }

  static <T> Optional<T> nonEmpty(T value) {
    return ofNullable(value).filter(not(OptionalUtils::isNullOrEmpty));
  }
}
