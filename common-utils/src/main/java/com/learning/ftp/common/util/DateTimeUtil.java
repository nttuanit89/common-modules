package com.learning.ftp.common.util;

import com.google.common.collect.ImmutableMap;
import com.learning.ftp.common.model.Holiday;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Map;
import static java.util.Objects.isNull;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class DateTimeUtil {
  public static final int YEAR = 0;
  public static final int MONTH = 1;
  public static final int WEEK = 2;
  public static final int HOUR = 3;
  public static final int MINUTE = 4;
  public static final int SECOND = 5;
  public static final int DAY = 6;
  public static final String PATTERN_DATE = "dd/MM/yyyy";
  public static final ZoneId ZONE_ID_VN = ZoneId.of("Asia/Ho_Chi_Minh");

  {
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
  }

  private static final Map<String, DayOfWeek> WEEKEND_DAYS_MAP =
      ImmutableMap.<String, DayOfWeek>builder()
          .put("MONDAY", DayOfWeek.MONDAY)
          .put("TUESDAY", DayOfWeek.TUESDAY)
          .put("WEDNESDAY", DayOfWeek.WEDNESDAY)
          .put("THURSDAY", DayOfWeek.THURSDAY)
          .put("FRIDAY", DayOfWeek.FRIDAY)
          .put("SATURDAY", DayOfWeek.SATURDAY)
          .put("SUNDAY", DayOfWeek.SUNDAY)
          .build();

  public static String getCurrentDateTimeInGMT7(String pattern) {
    return ZonedDateTime.now()
        .withZoneSameInstant(ZONE_ID_VN)
        .format(DateTimeFormatter.ofPattern(pattern));
  }

  public static String formatDateTimeInGMT7(Timestamp time, String pattern) {
    return LocalDateTime.ofInstant(time.toInstant(), ZONE_ID_VN)
        .format(DateTimeFormatter.ofPattern(pattern));
  }

  public static Optional<String> formatDateTimeInGMT7Op(Timestamp time, String pattern) {
    try {
      return Optional.ofNullable(time).map(t -> formatDateTimeInGMT7(t, pattern));
    } catch (Exception e) {
      return Optional.empty();
    }
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
            .withNano(0)
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
            .withNano(0)
            .toInstant()
            .toEpochMilli());
  }

  public static Timestamp convertDateToMidNight(Timestamp time) {
    return isNull(time) ? null : adjustDateTimeTo(time, 23, 59, 59);
  }

  public static Timestamp convertDateToStartDate(Timestamp time) {
    return isNull(time) ? null : adjustDateTimeTo(time, 0, 0, 0);
  }

  public static ZonedDateTime convertDateToStartDate(ZonedDateTime time) {
    return isNull(time) ? null : time.withZoneSameInstant(ZONE_ID_VN).truncatedTo(DAYS);
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

  public static ZonedDateTime addDaysSkippingWeekends(
      ZonedDateTime date, int days, List<Holiday> holidays, DayOfWeek... weekendDays) {
    ZonedDateTime result = date;
    int addedDays = 0;
    List<DayOfWeek> weekendDayList = new ArrayList<>();
    if (weekendDays == null || weekendDays.length == 0) {
      weekendDayList.add(DayOfWeek.SATURDAY);
      weekendDayList.add(DayOfWeek.SUNDAY);
    } else {
      weekendDayList.addAll(Arrays.asList(weekendDays));
    }
    while (addedDays < days) {
      result = result.plusDays(1);
      if (!weekendDayList.contains(result.getDayOfWeek()) && !isHoliday(result, holidays)) {
        ++addedDays;
      }
    }
    return result;
  }

  public static boolean isHoliday(ZonedDateTime date, List<Holiday> holidays) {
    long epochMilli = date.toInstant().toEpochMilli();
    return holidays.stream()
        .anyMatch(
            holiday -> epochMilli >= holiday.getStartDate() && epochMilli <= holiday.getEndDate());
  }

  public static boolean isWorkingDay(ZonedDateTime date, List<Holiday> holidays) {
    if (date.getDayOfWeek() == DayOfWeek.SUNDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY) {
      return false;
    }
    long epochMilli = date.toInstant().toEpochMilli();
    return holidays.stream()
        .noneMatch(
            holiday -> epochMilli >= holiday.getStartDate() && epochMilli <= holiday.getEndDate());
  }

  /**
   * Get next working day from current date, current date is in UTC+7
   *
   * @return next working day
   */
  public static ZonedDateTime getNextWorkingDayFromNow(
      List<Holiday> holidays, DayOfWeek... weekendDays) {
    return addDaysSkippingWeekends(ZonedDateTime.now(ZONE_ID_VN), 1, holidays, weekendDays);
  }

  /**
   * Get next working day from input date
   *
   * @return next working day
   */
  public static ZonedDateTime getNextWorkingDay(
      ZonedDateTime date, List<Holiday> holidays, DayOfWeek... weekendDays) {
    if (needMoveToNextWorkingDay(
        date, holidays, weekendDays.length > 0 ? List.of(weekendDays) : emptyList())) {
      return addDaysSkippingWeekends(date, 1, holidays, weekendDays);
    }
    return date;
  }

  public static boolean needMoveToNextWorkingDay(
      ZonedDateTime date, List<Holiday> holidays, List<DayOfWeek> weekendDays) {
    return isHoliday(date, holidays) || weekendDays.contains(date.getDayOfWeek());
  }

  public static Timestamp convertStringDateToTimestamp(String strDate, String pattern) {
    LocalDate date = LocalDate.parse(strDate, DateTimeFormatter.ofPattern(pattern));
    return new Timestamp(date.atStartOfDay(ZONE_ID_VN).toInstant().toEpochMilli());
  }

  public static Optional<Timestamp> convertStringDateToTimestampOp(String strDate, String pattern) {
    try {
      return Optional.ofNullable(strDate)
          .filter(StringUtils::isNoneBlank)
          .map(date -> convertStringDateToTimestamp(strDate, pattern));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static double getMonthDuration(Timestamp startDate, Timestamp endDate) {
    LocalDateTime localStartDate = DateTimeUtil.convertDateToStartDate(startDate).toLocalDateTime();
    LocalDateTime localEndDate = DateTimeUtil.convertDateToStartDate(endDate).toLocalDateTime();
    long months = ChronoUnit.MONTHS.between(localStartDate, localEndDate);
    long days = ChronoUnit.DAYS.between(localStartDate.plusMonths(months), localEndDate);
    return months + days / 30.0;
  }

  public static Timestamp toWorkingDate(
      Timestamp startDate, int nWorkingDay, List<Holiday>... holidayList) {
    int inc = nWorkingDay >= 0 ? 1 : -1;
    int limit = inc > 0 ? nWorkingDay : 0;
    int delta = inc > 0 ? 0 : -nWorkingDay;
    List<Holiday> holidays = holidayList.length > 0 ? holidayList[0] : emptyList();
    LocalDateTime localDateTime = LocalDateTime.ofInstant(startDate.toInstant(), ZONE_ID_VN);
    while (delta != limit) {
      localDateTime = localDateTime.plusDays(inc);
      if (isWorkingDay(localDateTime.atZone(ZONE_ID_VN), holidays)) {
        delta += inc;
      }
    }
    return new Timestamp(localDateTime.atZone(ZONE_ID_VN).toInstant().toEpochMilli());
  }

  public static boolean isBetweenDate(long date, long from, long to) {
    return !isBeforeDate(date, from) && !isAfterDate(date, to);
  }

  public static boolean isBeforeDate(long date1, long date2) {
    return convertDateToStartDate(new Timestamp(date1))
        .before(convertDateToStartDate(new Timestamp(date2)));
  }

  public static boolean isAfterDate(long date1, long date2) {
    return convertDateToStartDate(new Timestamp(date1))
        .after(convertDateToStartDate(new Timestamp(date2)));
  }

  public static boolean isEqualDate(long date1, long date2) {
    return convertDateToStartDate(new Timestamp(date1))
        .equals(convertDateToStartDate(new Timestamp(date2)));
  }

  public static List<DayOfWeek> convertToWeekendDays(List<String> weekendDays) {
    return weekendDays.stream()
        .map(k -> WEEKEND_DAYS_MAP.get(k.toUpperCase()))
        .collect(Collectors.toList());
  }

  public static Timestamp truncateTime(long timestamp) {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    long currentDate =
        Date.from(new Date(timestamp).toLocalDate().atStartOfDay(defaultZoneId).toInstant())
            .getTime();
    return new Timestamp(currentDate);
  }
}
