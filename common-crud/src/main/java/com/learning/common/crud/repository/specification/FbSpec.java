package com.learning.common.crud.repository.specification;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityGraph;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import static javax.persistence.criteria.JoinType.INNER;
import static javax.persistence.criteria.JoinType.LEFT;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.split;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

@SuppressWarnings({"rawtypes", "unchecked", "NullableProblems"})
@Value
@Builder
public class FbSpec<T> implements Specification<T> {

  @Singular List<Specification<T>> specs;
  EntityGraph graph;
  @Default Specification<T> defaultIfEmpty = falseCondition();

  @Override
  public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder builder) {
    return specs.stream()
        .filter(Objects::nonNull)
        .reduce(Specification::and)
        .map(spec -> spec.toPredicate(root, query, builder))
        .orElse(defaultIfEmpty.toPredicate(root, query, builder));
  }

  public interface FbCustomSpec<T> extends Specification<T> {
    @Nullable
    Predicate toFbPredicate(
        From<T, ?> attributes, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

    @Override
    @Nullable
    default Predicate toPredicate(
        Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
      return toFbPredicate(root, query, criteriaBuilder);
    }
  }

  public interface FbSubquery<T, S> {
    @Nullable
    Predicate toPredicate(
        From<T, ?> root,
        CriteriaQuery<?> query,
        From<S, ?> subRoot,
        Subquery<?> subquery,
        CriteriaBuilder criteriaBuilder);
  }

  static <T> FbCustomSpec<T> empty() {
    return (root, query, builder) -> null;
  }

  /**
   * Create a specification for join fetch.
   *
   * @param propertys entity's property names
   * @return equal specification
   */
  public static <T> FbCustomSpec<T> fetch(String... propertys) {
    return (root, query, builder) -> {
      if (!Long.class.equals(query.getResultType())) {
        Arrays.stream(propertys)
            .map(properties -> split(properties, "."))
            .forEach(
                properties -> {
                  Fetch<Object, Object> fetch = null;
                  for (String property : properties) {
                    fetch =
                        Objects.isNull(fetch)
                            ? root.fetch(property, LEFT)
                            : fetch.fetch(property, LEFT);
                  }
                });
        query.distinct(true);
      }
      return null;
    };
  }

  public static <T> FbCustomSpec<T> fetch(String property, FbCustomSpec<T> spec) {
    return fetch(property, INNER, spec);
  }

  public static <T> FbCustomSpec<T> fetch(String property, JoinType type, FbCustomSpec<T> spec) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<T, ?> fetch = (Join) root.fetch(property, type);
      return spec.toFbPredicate(fetch, query, builder);
    };
  }

  public static <T> FbCustomSpec<T> join(String property, FbCustomSpec<T> spec) {
    return join(property, INNER, spec);
  }

  public static <T> FbCustomSpec<T> join(String property, JoinType type, FbCustomSpec<T> spec) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<T, ?> fetch = root.join(property, type);
      return spec.toFbPredicate(fetch, query, builder);
    };
  }

  public static <R, S> FbCustomSpec<R> subquery(Class<S> entityClazz, FbSubquery<R, S> function) {
    return (root, query, builder) -> {
      Subquery<S> subquery = query.subquery(entityClazz);
      Root<S> subRoot = subquery.from(entityClazz);
      return function.toPredicate(root, query, subRoot, subquery, builder);
    };
  }

  @SuppressWarnings("rawtypes")
  public static boolean isNullOrEmpty(Object value) {
    return Objects.isNull(value)
        || (value instanceof String && isBlank(value.toString()))
        || (value instanceof Collection && ((Collection) value).isEmpty())
        || (value instanceof Map && ((Map) value).isEmpty());
  }

  public static <T> FbCustomSpec<T> falseCondition() {
    return (root, query, builder) -> builder.or();
  }

  public static <T> FbCustomSpec<T> trueCondition() {
    return (root, query, builder) -> builder.and();
  }

  public static <T> FbCustomSpec<T> in(String property, Object value) {
    return isNullOrEmpty(value) ? empty() : (root, query, builder) -> root.get(property).in(value);
  }

  public static <T> FbCustomSpec<T> nin(String property, Object value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> root.get(property).in(value).not();
  }

  public static <T> FbCustomSpec<T> isNull(String property) {
    return (root, query, builder) -> builder.isNull(root.get(property));
  }

  public static <T> FbCustomSpec<T> isEmpty(String property) {
    return (root, query, builder) -> builder.isEmpty(root.get(property));
  }

  /**
   * Create a specification for testing the value of the entity's property for equality.
   *
   * @param property entity's property name
   * @param value object
   * @return equal specification
   */
  public static <T> FbCustomSpec<T> eq(String property, Object value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.equal(root.get(property), value);
  }

  /**
   * Create a specification for testing the value of an entity's property for inequality.
   *
   * @param property entity's property name
   * @param value object
   * @return inequality specification
   */
  public static <T> FbCustomSpec<T> notEqual(String property, Object value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.notEqual(root.get(property), value);
  }

  /**
   * Create a specification for testing where the value of the entity's property is greater than the
   * given value.
   *
   * @param property entity's property name
   * @param value a number
   * @return greater-than specification
   */
  public static <T> FbCustomSpec<T> gt(String property, Number value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.gt(root.get(property), value);
  }

  public static <T> FbCustomSpec<T> gt(String property, Timestamp time) {
    if (isNullOrEmpty(time)) {
      return empty();
    }
    return (root, query, builder) -> builder.greaterThan(root.get(property), time);
  }

  /**
   * Create a specification for testing where the value of the entity's property is greater than or
   * equal to the given value.
   *
   * @param property entity's property name
   * @param value a number
   * @return greater-than-or-equal specification
   */
  public static <T> FbCustomSpec<T> ge(String property, Number value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.ge(root.get(property), value);
  }

  public static <T> FbCustomSpec<T> ge(String property, Timestamp value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(property), value);
  }

  /**
   * Create a specification for testing where the value of the entity's property is less than the
   * given value.
   *
   * @param property entity's property name
   * @param value a number
   * @return less-than specification
   */
  public static <T> FbCustomSpec<T> lt(String property, Number value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.lt(root.get(property), value);
  }

  public static <T> FbCustomSpec<T> lt(String property, Timestamp value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.lessThan(root.get(property), value);
  }

  /**
   * Create a specification for testing where the value of the entity's property is less than or
   * equal to the given value.
   *
   * @param property entity's property name
   * @param value a number
   * @return less-than-or-equal specification
   */
  public static <T> FbCustomSpec<T> le(String property, Number value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.le(root.get(property), value);
  }

  public static <T> FbCustomSpec<T> le(String property, Timestamp value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) -> builder.lessThanOrEqualTo(root.get(property), value);
  }

  /**
   * Create a specification for testing where the value of the entity's property is between the
   * given min and max value.
   *
   * @param property entity's property name
   * @param min long number
   * @param max long number
   * @return between specification
   */
  public static <T> FbCustomSpec<T> between(String property, Long min, Long max) {
    if (isNullOrEmpty(min) && isNullOrEmpty(max)) {
      return empty();
    }
    var minSafe = defaultIfNull(min, Long.MIN_VALUE);
    var maxSafe = defaultIfNull(max, Long.MAX_VALUE);
    return (root, query, builder) -> builder.between(root.get(property), minSafe, maxSafe);
  }

  /**
   * Create a specification for testing where the value of the entity's property is between the
   * given min and max date time.
   *
   * @param property entity's property name
   * @param min the lower bound {@link ZonedDateTime} number
   * @param max the upper bound {@link ZonedDateTime} number
   * @return between specification
   */
  public static <T> FbCustomSpec<T> between(String property, ZonedDateTime min, ZonedDateTime max) {
    if (isNullOrEmpty(min) && isNullOrEmpty(max)) {
      return empty();
    }
    var maxSafe = defaultIfNull(max, ZonedDateTime.now());
    var minSafe = defaultIfNull(min, maxSafe.minusDays(14));
    return (root, query, builder) -> builder.between(root.get(property), minSafe, maxSafe);
  }

  /**
   * Create a specification for testing where the value of the entity's property is between the
   * given min and max value.
   *
   * @param property entity's property name
   * @param min long number
   * @param max long number
   * @return between specification
   */
  public static <T> FbCustomSpec<T> between(String property, Timestamp min, Timestamp max) {
    if (isNullOrEmpty(min) && isNullOrEmpty(max)) {
      return empty();
    }
    if (isNullOrEmpty(min)) {
      return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(property), max);
    }
    if (isNullOrEmpty(max)) {
      return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(property), min);
    }
    return (root, query, builder) -> builder.between(root.get(property), min, max);
  }

  /**
   * Create a specification for testing where the string value of the entity's property is like the
   * pattern "%" + lowercase given string value + "%"
   *
   * @param property entity's property name
   * @param value string value
   * @return like specification
   */
  public static <T> FbCustomSpec<T> like(String property, String value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) ->
            builder.like(builder.lower(root.get(property)), "%" + value.toLowerCase() + "%");
  }

  /**
   * Create a specification for testing where the lower case string value of the entity's property
   * is like the pattern lowercase given string value + "%"
   *
   * @param property entity's property name
   * @param value string value
   * @return like specification
   */
  public static <T> FbCustomSpec<T> startWith(String property, String value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) ->
            builder.like(builder.lower(root.get(property)), value.toLowerCase() + "%");
  }

  /**
   * Create a specification for testing where the lower case string value of the entity's property
   * is like the pattern "%" + lowercase given string value
   *
   * @param property entity's property name
   * @param value string value
   * @return like specification
   */
  public static <T> FbCustomSpec<T> endWith(String property, String value) {
    return isNullOrEmpty(value)
        ? empty()
        : (root, query, builder) ->
            builder.like(builder.lower(root.get(property)), "%" + value.toLowerCase());
  }
}
