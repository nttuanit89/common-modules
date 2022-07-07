package com.learning.ftp.common.crud.condition;

import com.learning.ftp.common.crud.finding.QueryContext;
import com.learning.ftp.common.crud.util.DataTypeUtil;
import com.learning.ftp.common.crud.util.EntityUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.learning.ftp.common.crud.util.EntityPathUtil.getFieldPath;
import static com.learning.ftp.common.crud.util.EntityPathUtil.getJsonFieldPath;
import static com.learning.ftp.common.crud.util.EntityPathUtil.isJsonField;

@SuppressWarnings("unchecked")
@Getter
@Setter
public class Condition {
  private String fieldName;
  private Object value;
  private Object value2;
  private PredicateGenerator predicateGenerator;

  protected Condition() {}

  private Condition(String fieldName) {
    this.fieldName = fieldName;
  }

  public static Condition field(String fieldName) {
    return new Condition(fieldName);
  }

  public Predicate toPredicate(QueryContext queryContext) {
    return predicateGenerator.gen(queryContext);
  }

  public Condition like(String value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) ->
            queryContext
                .getCriteriaBuilder()
                .like(
                    queryContext.getCriteriaBuilder().lower(getFieldPath(queryContext, fieldName)),
                    String.format("%%%s%%", value.toLowerCase().replaceAll("%", "\\%")));
    return this;
  }

  public Condition equal(Object value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator = (queryContext) -> createEqualPredicate(queryContext, value);
    return this;
  }

  public Condition in(Collection<?> range) {
    if (range == null || range.isEmpty()) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) -> {
          Class fieldType = EntityUtils.getFieldType(queryContext.getEntityClass(), fieldName);
          Collection<?> newValues =
              range.stream()
                  .map(v -> DataTypeUtil.convertData(v, fieldType))
                  .collect(Collectors.toList());
          return getFieldPath(queryContext, fieldName).in(newValues);
        };
    return this;
  }

  public Condition contains(String value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) -> {
          Class fieldType = EntityUtils.getFieldType(queryContext.getEntityClass(), fieldName);
          Class argumentType =
              fieldType.isArray()
                  ? fieldType.getComponentType()
                  : EntityUtils.getEntityArgumentType(
                      EntityUtils.getField(queryContext.getEntityClass(), fieldName));
          return queryContext
              .getCriteriaBuilder()
              .isMember(
                  (Object) DataTypeUtil.convertData(value, argumentType),
                  getFieldPath(queryContext, fieldName));
        };
    return this;
  }

  public Condition notIn(Collection<?> range) {
    if (range == null || range.isEmpty()) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) -> {
          Class fieldType = EntityUtils.getFieldType(queryContext.getEntityClass(), fieldName);
          Collection<?> newValues =
              range.stream()
                  .map(v -> DataTypeUtil.convertData(v, fieldType))
                  .collect(Collectors.toList());
          return queryContext
              .getCriteriaBuilder()
              .not(getFieldPath(queryContext, fieldName).in(newValues));
        };
    return this;
  }

  public Condition isNull() {
    this.predicateGenerator =
        (queryContext) ->
            queryContext.getCriteriaBuilder().isNull(getFieldPath(queryContext, fieldName));
    return this;
  }

  public Condition isNotNull() {
    this.predicateGenerator =
        (queryContext) ->
            queryContext.getCriteriaBuilder().isNotNull(getFieldPath(queryContext, fieldName));
    return this;
  }

  public Condition greaterThan(Object value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) ->
            ComparisionPredicateGenerator.genGreaterThan(queryContext, fieldName, value);
    return this;
  }

  public Condition greaterThanOrEqual(Object value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) ->
            ComparisionPredicateGenerator.genGreaterThanOrEqual(queryContext, fieldName, value);
    return this;
  }

  public Condition lt(String otherField) {
    if (StringUtils.isBlank(otherField)) {
      return null;
    }

    this.predicateGenerator =
        (qc) ->
            qc.getCriteriaBuilder()
                .lessThan(getFieldPath(qc, fieldName), getFieldPath(qc, otherField));
    return this;
  }

  public Condition lessThan(Object value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) -> ComparisionPredicateGenerator.genLessThan(queryContext, fieldName, value);
    return this;
  }

  public Condition lessThanOrEqual(Object value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator =
        (queryContext) ->
            ComparisionPredicateGenerator.genLessThanOrEqual(queryContext, fieldName, value);
    return this;
  }

  public Condition between(Object start, Object end) {
    if ((start == null && end == null)) {
      return null;
    }

    if (start == null) {
      return lessThanOrEqual(end);
    }

    if (end == null) {
      return greaterThanOrEqual(start);
    }

    this.predicateGenerator =
        (queryContext) ->
            ComparisionPredicateGenerator.genBetween(queryContext, fieldName, start, end);
    return this;
  }

  public Condition notEqual(Object value) {
    if (value == null) {
      return null;
    }
    this.predicateGenerator = (queryContext) -> createNotEqualPredicate(queryContext, value);
    return this;
  }

  private Predicate createEqualPredicate(QueryContext qc, Object value) {
    return createEqualOrNotEqualPredicate(qc, value);
  }

  private Predicate createNotEqualPredicate(QueryContext qc, Object value, boolean... isNotEqual) {
    return createEqualOrNotEqualPredicate(qc, value, true);
  }

  private Predicate createEqualOrNotEqualPredicate(
      QueryContext qc, Object value, boolean... isNotEqual) {
    if (isJsonField(this.fieldName)) {
      if (isNotEqual.length < 1 || !isNotEqual[0]) {
        return qc.getCriteriaBuilder().equal(getJsonFieldPath(qc, this.fieldName), value);
      }

      return qc.getCriteriaBuilder().notEqual(getJsonFieldPath(qc, this.fieldName), value);
    }

    Class fieldType = EntityUtils.getFieldType(qc.getEntityClass(), fieldName);
    String realFieldName = fieldName;
    if (EntityUtils.isEntityClass(fieldType) && EntityUtils.isEntity(value)) {
      String idFieldName = EntityUtils.getIdFieldName(fieldType);
      fieldType = EntityUtils.getFieldType(fieldType, idFieldName);
      value = EntityUtils.getId(value);
      realFieldName = String.format("%s.%s", fieldName, idFieldName);
    }
    if (isNotEqual.length < 1 || !isNotEqual[0]) {
      if (CharSequence.class.isAssignableFrom(fieldType)) {
        return qc.getCriteriaBuilder()
            .equal(
                qc.getCriteriaBuilder().lower(getFieldPath(qc, realFieldName)),
                DataTypeUtil.convertData(value, fieldType).toString().toLowerCase());
      }
      return qc.getCriteriaBuilder()
          .equal(
              getFieldPath(qc, realFieldName), (Object) DataTypeUtil.convertData(value, fieldType));
    }
    if (CharSequence.class.isAssignableFrom(fieldType)) {
      return qc.getCriteriaBuilder()
          .notEqual(
              qc.getCriteriaBuilder().lower(getFieldPath(qc, realFieldName)),
              DataTypeUtil.convertData(value, fieldType).toString().toLowerCase());
    }
    return qc.getCriteriaBuilder()
        .notEqual(
            getFieldPath(qc, realFieldName), (Object) DataTypeUtil.convertData(value, fieldType));
  }

  @FunctionalInterface
  private interface PredicateGenerator {
    Predicate gen(QueryContext queryContext);
  }
}
