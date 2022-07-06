package com.learning.common.util;

import java.util.Objects;

public class StringUtils {
  private StringUtils() {
    throw new UnsupportedOperationException();
  }

  public static String stripAccents(String value) {
    if (Objects.isNull(value)) {
      return null;
    }

    String replaceD = value.replace("đ", "d").replace("Đ", "D");
    return org.apache.commons.lang3.StringUtils.stripAccents(replaceD);
  }
}
