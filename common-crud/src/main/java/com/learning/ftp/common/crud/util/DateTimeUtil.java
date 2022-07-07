package com.learning.ftp.common.crud.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Deprecated
public class DateTimeUtil {
  public static final int YEAR = 0;
  public static final int MONTH = 1;
  public static final int WEEK = 2;
  public static final int HOUR = 3;
  public static final int MINUTE = 4;
  public static final int SECOND = 5;
  public static final int DAY = 6;
  private static final ZoneId ZONE_ID_VN = ZoneId.of("Asia/Ho_Chi_Minh");

  public static String getCurrentDateTimeInGMT7(String pattern) {
    return ZonedDateTime.now()
        .withZoneSameInstant(ZONE_ID_VN)
        .format(DateTimeFormatter.ofPattern(pattern));
  }

  public static String formatDateTimeInGMT7(Timestamp time, String pattern) {
    return LocalDateTime.ofInstant(time.toInstant(), ZONE_ID_VN)
        .format(DateTimeFormatter.ofPattern(pattern));
  }

  public static Timestamp getCurrentDateTimeInGMT7() {
    return new Timestamp(
        ZonedDateTime.now().withZoneSameInstant(ZONE_ID_VN).toInstant().toEpochMilli());
  }

  public static Timestamp getCurrentDateInGMT7() {
    return new Timestamp(
        ZonedDateTime.now()
            .withZoneSameInstant(ZONE_ID_VN)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .toInstant()
            .toEpochMilli());
  }

  public static Timestamp getEndOfCurrentDateInGMT7() {
    return new Timestamp(
        ZonedDateTime.now()
            .withZoneSameInstant(ZONE_ID_VN)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .toInstant()
            .toEpochMilli());
  }

  public static Timestamp convertDateToMidNight(Timestamp time) {
    return adjustDateTimeTo(time, 23, 59, 59);
  }

  public static Timestamp convertDateToStartDate(Timestamp time) {
    return adjustDateTimeTo(time, 0, 0, 0);
  }

  public static Timestamp adjustDateTimeTo(Timestamp time, int hour, int min, int sec) {
    LocalDateTime localDateTime = LocalDateTime.ofInstant(time.toInstant(), ZONE_ID_VN);
    localDateTime = localDateTime.withHour(hour).withMinute(min).withSecond(sec).withNano(0);
    return new Timestamp(localDateTime.atZone(ZONE_ID_VN).toInstant().toEpochMilli());
  }

  public static Timestamp convertToGMT7(Timestamp time) {
    LocalDateTime localDateTime = time.toLocalDateTime();
    return new Timestamp(localDateTime.atZone(ZONE_ID_VN).toInstant().toEpochMilli());
  }

  public static Timestamp adjust(Timestamp time, int field, int d) {
    LocalDateTime localDateTime = LocalDateTime.ofInstant(time.toInstant(), ZONE_ID_VN);

    switch (field) {
      case YEAR:
        localDateTime = localDateTime.plusYears(d);
        break;
      case MONTH:
        localDateTime = localDateTime.plusMonths(d);
        break;
      case WEEK:
        localDateTime = localDateTime.plusWeeks(d);
        break;
      case HOUR:
        localDateTime = localDateTime.plusHours(d);
        break;
      case MINUTE:
        localDateTime = localDateTime.plusMinutes(d);
        break;
      case SECOND:
        localDateTime = localDateTime.plusSeconds(d);
        break;
      case DAY:
        localDateTime = localDateTime.plusDays(d);
        break;
    }
    return new Timestamp(localDateTime.atZone(ZONE_ID_VN).toInstant().toEpochMilli());
  }
}
