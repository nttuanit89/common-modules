package com.learning.common.mapper;

import static com.learning.common.util.DateTimeUtil.PATTERN_DATE;
import static com.learning.common.util.DateTimeUtil.convertStringDateToTimestampOp;
import static com.learning.common.util.DateTimeUtil.formatDateTimeInGMT7Op;
import java.sql.Timestamp;
import static java.util.Optional.ofNullable;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@SuppressWarnings("unused")
@Mapper(componentModel = "spring")
public abstract class DateTimeMapper {
  public static final String LONG_FROM_DEFAULT_DATE_FORMAT = "longFromDefaultDateFormat";
  public static final String TO_DEFAULT_DATE_FORMAT = "toDefaultDateFormat";
  public static final String FROM_DEFAULT_DATE_FORMAT = "fromDefaultDateFormat";
  public static final String LONG_TO_DEFAULT_DATE_FORMAT = "longToDefaultDateFormat";
  @Autowired private SimpleTypeMapper stm;

  @Named(LONG_TO_DEFAULT_DATE_FORMAT)
  public String longToDefaultDateFormat(Long date) {
    return toDefaultDateFormat(stm.toTimestamp(date));
  }

  @Named(LONG_FROM_DEFAULT_DATE_FORMAT)
  public Long longFromDefaultDateFormat(String date) {
    return stm.toLong(fromDefaultDateFormat(date));
  }

  @Named(TO_DEFAULT_DATE_FORMAT)
  public String toDefaultDateFormat(Timestamp date) {
    return formatDateTimeInGMT7Op(date, PATTERN_DATE).orElse("");
  }

  @Named(FROM_DEFAULT_DATE_FORMAT)
  public Timestamp fromDefaultDateFormat(String date) {
    return ofNullable(date)
        .filter(StringUtils::hasText)
        .flatMap(d -> convertStringDateToTimestampOp(d, PATTERN_DATE))
        .orElse(null);
  }
}
