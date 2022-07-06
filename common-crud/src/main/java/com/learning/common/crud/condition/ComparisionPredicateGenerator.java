package com.learning.common.crud.condition;

import com.learning.common.crud.finding.QueryContext;
import com.learning.common.crud.util.DataTypeUtil;
import com.learning.common.crud.util.EntityPathUtil;
import com.learning.common.crud.util.EntityUtils;

import java.sql.Timestamp;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

public class ComparisionPredicateGenerator {
  public static Predicate genGreaterThan(
      QueryContext qc, String fieldName, Object value, Object... moreVal) {
    Class fieldType = EntityUtils.getFieldType(qc.getEntityClass(), fieldName);
    return genPredicate(qc, "gt", fieldType, fieldName, value, moreVal);
  }

  public static Predicate genGreaterThanOrEqual(
      QueryContext qc, String fieldName, Object value, Object... moreVal) {
    Class fieldType = EntityUtils.getFieldType(qc.getEntityClass(), fieldName);
    return genPredicate(qc, "gte", fieldType, fieldName, value, moreVal);
  }

  public static Predicate genLessThan(
      QueryContext qc, String fieldName, Object value, Object... moreVal) {
    Class fieldType = EntityUtils.getFieldType(qc.getEntityClass(), fieldName);
    return genPredicate(qc, "lt", fieldType, fieldName, value, moreVal);
  }

  public static Predicate genLessThanOrEqual(
      QueryContext qc, String fieldName, Object value, Object... moreVal) {
    Class fieldType = EntityUtils.getFieldType(qc.getEntityClass(), fieldName);
    return genPredicate(qc, "lte", fieldType, fieldName, value, moreVal);
  }

  public static Predicate genBetween(
      QueryContext qc, String fieldName, Object value, Object... moreVal) {
    Class fieldType = EntityUtils.getFieldType(qc.getEntityClass(), fieldName);
    return genPredicate(qc, "bt", fieldType, fieldName, value, moreVal);
  }

  private static Predicate genPredicate(
      QueryContext qc,
      String op,
      Class fieldType,
      String fieldName,
      Object value,
      Object... moreVal) {
    switch (fieldType.getSimpleName()) {
      case "Timestamp":
        return genPredicateTimestamp(qc, fieldType, op, fieldName, value, moreVal);
      case "Integer":
      case "int":
        return genPredicateInteger(qc, fieldType, op, fieldName, value, moreVal);
      case "Long":
      case "long":
        return genPredicateLong(qc, fieldType, op, fieldName, value, moreVal);
      case "Double":
      case "double":
        return genPredicateDouble(qc, fieldType, op, fieldName, value, moreVal);
    }
    return null;
  }

  private static Predicate genPredicateTimestamp(
      QueryContext qc,
      Class fieldType,
      String op,
      String fieldName,
      Object value,
      Object... moreVal) {
    Expression<Timestamp> fieldTypeExp = EntityPathUtil.getFieldPath(qc, fieldName);
    Timestamp timeValue = DataTypeUtil.convertData(value, fieldType);
    Timestamp timMoreVal =
        moreVal.length > 0 ? (Timestamp) DataTypeUtil.convertData(moreVal[0], fieldType) : null;
    switch (op) {
      case "gt":
        return qc.getCriteriaBuilder().greaterThan(fieldTypeExp, timeValue);
      case "gte":
        return qc.getCriteriaBuilder().greaterThanOrEqualTo(fieldTypeExp, timeValue);
      case "lt":
        return qc.getCriteriaBuilder().lessThan(fieldTypeExp, timeValue);
      case "lte":
        return qc.getCriteriaBuilder().lessThanOrEqualTo(fieldTypeExp, timeValue);
      case "bt":
        return qc.getCriteriaBuilder().between(fieldTypeExp, timeValue, timMoreVal);
    }
    return null;
  }

  private static Predicate genPredicateInteger(
      QueryContext qc,
      Class fieldType,
      String op,
      String fieldName,
      Object value,
      Object... moreVal) {
    Expression<Integer> fieldTypeExp = EntityPathUtil.getFieldPath(qc, fieldName);
    Integer timeValue = DataTypeUtil.convertData(value, fieldType);
    Integer timMoreVal =
        moreVal.length > 0 ? DataTypeUtil.convertData(moreVal[0], fieldType) : null;
    switch (op) {
      case "gt":
        return qc.getCriteriaBuilder().greaterThan(fieldTypeExp, timeValue);
      case "gte":
        return qc.getCriteriaBuilder().greaterThanOrEqualTo(fieldTypeExp, timeValue);
      case "lt":
        return qc.getCriteriaBuilder().lessThan(fieldTypeExp, timeValue);
      case "lte":
        return qc.getCriteriaBuilder().lessThanOrEqualTo(fieldTypeExp, timeValue);
      case "bt":
        return qc.getCriteriaBuilder().between(fieldTypeExp, timeValue, timMoreVal);
    }
    return null;
  }

  private static Predicate genPredicateLong(
      QueryContext qc,
      Class fieldType,
      String op,
      String fieldName,
      Object value,
      Object... moreVal) {
    Expression<Long> fieldTypeExp = EntityPathUtil.getFieldPath(qc, fieldName);
    Long longValue = DataTypeUtil.convertData(value, fieldType);
    Long longMoreVal = moreVal.length > 0 ? DataTypeUtil.convertData(moreVal[0], fieldType) : null;
    switch (op) {
      case "gt":
        return qc.getCriteriaBuilder().greaterThan(fieldTypeExp, longValue);
      case "gte":
        return qc.getCriteriaBuilder().greaterThanOrEqualTo(fieldTypeExp, longValue);
      case "lt":
        return qc.getCriteriaBuilder().lessThan(fieldTypeExp, longValue);
      case "lte":
        return qc.getCriteriaBuilder().lessThanOrEqualTo(fieldTypeExp, longValue);
      case "bt":
        return qc.getCriteriaBuilder().between(fieldTypeExp, longValue, longMoreVal);
    }
    return null;
  }

  public static Predicate genPredicateDouble(
      QueryContext qc,
      Class fieldType,
      String op,
      String fieldName,
      Object value,
      Object... moreVal) {
    Expression<Double> fieldTypeExp = EntityPathUtil.getFieldPath(qc, fieldName);
    Double doubleValue = DataTypeUtil.convertData(value, fieldType);
    Double doubleMoreVal =
        moreVal.length > 0 ? DataTypeUtil.convertData(moreVal[0], fieldType) : null;
    switch (op) {
      case "gt":
        return qc.getCriteriaBuilder().greaterThan(fieldTypeExp, doubleValue);
      case "gte":
        return qc.getCriteriaBuilder().greaterThanOrEqualTo(fieldTypeExp, doubleValue);
      case "lt":
        return qc.getCriteriaBuilder().lessThan(fieldTypeExp, doubleValue);
      case "lte":
        return qc.getCriteriaBuilder().lessThanOrEqualTo(fieldTypeExp, doubleValue);
      case "bt":
        return qc.getCriteriaBuilder().between(fieldTypeExp, doubleValue, doubleMoreVal);
    }
    return null;
  }
}
