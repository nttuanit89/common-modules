package com.learning.common.crud.finding;

import com.learning.common.crud.util.EntityPathUtil;
import java.util.Arrays;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import lombok.Getter;

@Getter
public class Sort {
  private String field;
  private boolean isAsc;

  private Sort(String field, boolean isAsc) {
    this.field = field;
    this.isAsc = isAsc;
  }
  public static Sort asc(String field) {
    return new Sort(field, true);
  }
  public static Sort desc(String field) {
    return new Sort(field, false);
  }
  public static Sort[] ascList(String... field) {
    if (field == null || field.length == 0) {
      throw new RuntimeException("Sort fields must not empty");
    }
    return Arrays.stream(field).map(f -> new Sort(f, true)).toArray(size -> new Sort[size]);
  }
  public static Sort[] descList(String... field) {
    if (field == null || field.length == 0) {
      throw new RuntimeException("Sort fields must not empty");
    }
    return Arrays.stream(field).map(f -> new Sort(f, false)).toArray(size -> new Sort[size]);
  }

  public Expression toExpression(QueryContext qc) {
    if (field.contains("+")) {
      return toSumExpression(qc);
    }
    if (field.contains("-")) {
      return toSubtractExpression(qc);
    }
    if (field.contains("*")) {
      return toMultipleExpression(qc);
    }
    return EntityPathUtil.getFieldPath(qc, field);
  }
  private Expression toSumExpression(QueryContext qc) {
    String[] parts = field.split("\\s*\\+\\s*");
    CriteriaBuilder cb = qc.getCriteriaBuilder();

    Expression sum = cb.sum(EntityPathUtil.getFieldPath(qc, parts[0]), EntityPathUtil.getFieldPath(qc, parts[1]));
    for (int i = 2; i < parts.length; ++i) {
      sum = cb.sum(sum, EntityPathUtil.getFieldPath(qc, parts[i]));
    }
    return sum;
  }
  private Expression toSubtractExpression(QueryContext qc) {
    String[] parts = field.split("\\s\\*-\\s*");
    CriteriaBuilder cb = qc.getCriteriaBuilder();

    Expression substraction = cb.diff(EntityPathUtil.getFieldPath(qc, parts[0]), EntityPathUtil.getFieldPath(qc, parts[1]));
    for (int i = 2; i < parts.length; ++i) {
      substraction = cb.diff(substraction, EntityPathUtil.getFieldPath(qc, parts[i]));
    }
    return substraction;
  }
  private Expression toMultipleExpression(QueryContext qc) {
    String[] parts = field.split("\\s*-\\s*");
    CriteriaBuilder cb = qc.getCriteriaBuilder();

    Expression multipleExp = cb.prod(EntityPathUtil.getFieldPath(qc, parts[0]), EntityPathUtil.getFieldPath(qc, parts[1]));
    for (int i = 2; i < parts.length; ++i) {
      multipleExp = cb.prod(multipleExp, EntityPathUtil.getFieldPath(qc, parts[i]));
    }
    return multipleExp;
  }
}
