package com.learning.ftp.common.crud.util;

import com.learning.ftp.common.util.UUIDUtils;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

public class DataTypeUtil {

  public static <T> T convertData(Object value, Class valType) {
    try {
      if (value == null) {
        return null;
      }
      if (valType.isEnum()) {
        return (T) getEnumValue(valType, value);
      }
      String simpleName = valType.getSimpleName();
      switch (simpleName) {
        case "String":
          return (T) value.toString();
        case "UUID":
          return (T) UUIDUtils.fromStringSafe(value.toString());
        case "Timestamp":
          return (T) convertTimestampValue(value);
        case "Boolean":
          return !StringUtils.isEmpty(value) ? (T) Boolean.valueOf(value.toString()) : null;
        case "Integer":
          return !StringUtils.isEmpty(value)
              ? (T) new Integer(Double.valueOf(value.toString()).intValue())
              : null;
        case "Long":
          return !StringUtils.isEmpty(value)
              ? (T) new Long(Double.valueOf(value.toString()).longValue())
              : null;
        case "Double":
          return !StringUtils.isEmpty(value) ? (T) Double.valueOf(value.toString()) : null;
        case "Float":
          return !StringUtils.isEmpty(value)
              ? (T) new Float(Double.valueOf(value.toString()).floatValue())
              : null;
        default:
          if (valType.isPrimitive() && Character.isLowerCase(simpleName.charAt(0))) {
            if ("int".equals(simpleName)) {
              simpleName = "Integer";
            }
            valType =
                DataTypeUtil.class
                    .getClassLoader()
                    .loadClass(String.format("java.lang.%s", StringUtils.capitalize(simpleName)));
            return convertData(value, valType);
          }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return (T) value;
  }

  private static Timestamp convertTimestampValue(Object val) {
    if (StringUtils.isEmpty(val)) {
      return null;
    }
    if (val instanceof Timestamp) {
      return (Timestamp) val;
    }
    Function<Object, Timestamp> c1 =
        v -> {
          try {
            return Timestamp.valueOf(v.toString());
          } catch (Exception ex) {
            return null;
          }
        };
    Function<Object, Timestamp> c2 =
        v ->
            Stream.of("dd/MM/yyyy", "d/M/yyyy", "dd/MM/yyyy HH:mm:ss", "d/M/yyyy HH:mm:ss")
                .map(
                    f -> {
                      try {
                        return new Timestamp(
                            new SimpleDateFormat(f).parse(val.toString()).getTime());
                      } catch (Exception ex) {
                        return null;
                      }
                    })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    Function<Object, Timestamp> c3 =
        v -> {
          try {
            return new Timestamp(Long.parseLong(v.toString()));
          } catch (Exception ex) {
            return null;
          }
        };
    return Stream.of(c1, c2, c3)
        .map(c -> c.apply(val))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Object getEnumValue(Class enumType, Object value) throws Exception {
    if (enumType.isAssignableFrom(value.getClass())) {
      return value;
    }
    if (value instanceof String) {
      return Enum.valueOf(enumType, value.toString());
    }
    return null;
  }
}
