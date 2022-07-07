package com.learning.ftp.common.util;

import java.util.Optional;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class UUIDUtils {
  private UUIDUtils() {}

  /** Null-safe UUID toString */
  public static String toString(UUID uuid) {
    // JDK 11 UUID::toString is optimized already
    return ofNullable(uuid).map(UUID::toString).orElse(null);
  }

  public static UUID fromStringSafe(String uuid) {
    return fromString(uuid).orElse(null);
  }

  /** Optimized UUID creation */
  public static Optional<UUID> fromString(String uuid) {
    // JDK 17 UUID::fromString is optimized already
    try {
      return ofNullable(uuid).filter(StringUtils::isNotBlank).map(UUID::fromString);
    } catch (IllegalArgumentException e) {
      return empty();
    }
  }
}
