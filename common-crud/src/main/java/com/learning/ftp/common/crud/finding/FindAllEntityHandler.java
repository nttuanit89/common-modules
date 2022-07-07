package com.learning.ftp.common.crud.finding;

import com.learning.ftp.common.crud.condition.Condition;
import java.util.List;
import javax.persistence.criteria.JoinType;

public class FindAllEntityHandler<T> extends AbstractFindEntityHandler<T> {
  public FindAllEntityHandler(Class clazz, String[] fields, FindContext findContext) {
    super(clazz, fields, findContext);
  }

  @Override
  public FindAllEntityHandler<T> withCondition(Condition condition) {
    super.withCondition(condition);
    return this;
  }

  @Override
  public FindAllEntityHandler<T> withJoinType(JoinType joinType) {
    super.withJoinType(joinType);
    return this;
  }

  @Override
  public FindAllEntityHandler<T> setEmptyLazyFields() {
    super.setEmptyLazyFields();
    return this;
  }

  public List<T> execute() {
    return getListResult();
  }

  @Override
  public FindAllEntityHandler<T> sortBy(Sort... sort) {
    super.sortBy(sort);
    return this;
  }
}
