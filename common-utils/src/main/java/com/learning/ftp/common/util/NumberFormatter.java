package com.learning.ftp.common.util;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * Utility class for formatting number.
 *
 * @author Thuan Xinh
 */
public class NumberFormatter {
  private static final String DECIMAL_SEPARATOR_PATTERN = "#,###.##";

  /**
   * Format a number to string with thousand separator.
   *
   * @param number number object
   * @param pattern format pattern
   * @return a formatted number, empty if the number is null
   */
  public static String format(Object number, String pattern) {
    if (number == null) {
      return "";
    }
    DecimalFormat decimalFormat = new DecimalFormat(pattern);
    if (number instanceof String) {
      try {
        number = decimalFormat.parseObject((String) number);
      } catch (ParseException e) {
        return "";
      }
    }
    return decimalFormat.format(number);
  }

  /**
   * Format a number to string with thousand separator.
   *
   * @param number number object
   * @return a formatted number, empty if the number is null
   */
  public static String formatWithSeparator(Object number) {
    return format(number, DECIMAL_SEPARATOR_PATTERN);
  }
}
