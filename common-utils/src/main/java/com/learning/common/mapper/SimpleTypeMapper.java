package com.learning.common.mapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import static com.learning.common.util.DateTimeUtil.ZONE_ID_VN;
import com.learning.common.util.JsonMapper;
import com.learning.common.util.UUIDUtils;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import java.util.UUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.apache.logging.log4j.util.Strings;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@SuppressWarnings("unused")
@Mapper(componentModel = "spring")
public interface SimpleTypeMapper {

  @Named("emptyToNull")
  default String emptyToNull(String str) {
    return com.google.common.base.Strings.emptyToNull(str);
  }

  default String uuidToString(UUID value) {
    return UUIDUtils.toString(value);
  }

  default UUID toUUID(String value) {
    return UUIDUtils.fromStringSafe(value);
  }

  default List<UUID> toUUIDs(Collection<String> values) {
    Collection<String> list = ofNullable(values).orElse(List.of());
    return list.stream().map(this::toUUID).filter(Objects::nonNull).collect(toList());
  }

  default Long toLong(Timestamp ts) {
    return ofNullable(ts).map(Timestamp::getTime).orElse(null);
  }

  default Timestamp toTimestamp(Long ts) {
    return ofNullable(ts)
        .filter(t -> t != 0L)
        .map(Instant::ofEpochMilli)
        .map(Timestamp::from)
        .orElse(null);
  }

  default ZonedDateTime toZdt(Long ts) {
    return ofNullable(ts)
        .filter(t -> t != 0)
        .map(Instant::ofEpochMilli)
        .map(t -> t.atZone(ZONE_ID_VN))
        .orElse(null);
  }

  default Boolean toBoolean(String boolType) {
    return isEmpty(boolType) ? null : Boolean.valueOf(boolType);
  }

  default String toStringBoolean(Boolean boolType) {
    return isNull(boolType) ? Strings.EMPTY : boolType.toString();
  }

  default BigDecimal toBigDecimal(String value) {
    return ofNullable(value)
        .filter(org.apache.commons.lang3.StringUtils::isNumeric)
        .map(BigDecimal::new)
        .orElse(null);
  }

  default <V> List<V> filterNonNull(List<V> original) {
    List<V> list = ofNullable(original).orElse(ImmutableList.of());
    ImmutableList.Builder<V> builder = ImmutableList.builder();
    list.stream().filter(Objects::nonNull).forEach(builder::add);
    return builder.build();
  }

  default <K, V> Map<K, V> filterNonNull(Map<K, V> original) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    Map<K, V> map = ofNullable(original).orElse(ImmutableMap.of());
    map.entrySet().stream()
        .filter(Objects::nonNull)
        .filter(e -> nonNull(e.getKey()))
        .filter(e -> nonNull(e.getValue()))
        .forEach(builder::put);

    return builder.build();
  }

  default Map<String, String> filterNonNullToString(Map<?, ?> original) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    Map<?, ?> map = ofNullable(original).orElse(ImmutableMap.of());
    map.entrySet().stream()
        .filter(Objects::nonNull)
        .filter(e -> nonNull(e.getKey()))
        .filter(e -> nonNull(e.getValue()))
        .map(e -> Maps.immutableEntry(String.valueOf(e.getKey()), String.valueOf(e.getValue())))
        .forEach(builder::put);
    return builder.build();
  }

  default Map<String, String> toMapSS(Object object) {
    return filterNonNullToString(JsonMapper.getMapper().convertValue(object, Map.class));
  }

  default Map<String, Object> filterNonNullToObj(Map<?, ?> original) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    Map<?, ?> map = ofNullable(original).orElse(ImmutableMap.of());
    map.entrySet().stream()
        .filter(Objects::nonNull)
        .filter(e -> nonNull(e.getKey()))
        .filter(e -> nonNull(e.getValue()))
        .map(e -> Maps.immutableEntry(String.valueOf(e.getKey()), e.getValue()))
        .forEach(builder::put);
    return builder.build();
  }
}
