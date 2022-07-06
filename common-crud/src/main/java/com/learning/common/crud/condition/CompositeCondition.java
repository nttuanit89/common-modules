package com.learning.common.crud.condition;

import com.learning.common.crud.finding.QueryContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.criteria.Predicate;

class CompositeCondition extends Condition {
  private List<Condition> conditions;
  private boolean isAnd;

  public CompositeCondition(boolean isAnd, Condition... conditions) {
    this.isAnd = isAnd;
    this.conditions = new ArrayList<>(Arrays.asList(conditions));
  }

  @Override
  public Predicate toPredicate(QueryContext qc) {
    Predicate[] predicates =
        conditions.stream().filter(Objects::nonNull).map(c -> c.toPredicate(qc)).toArray(size -> new Predicate[size]);
    return isAnd ? qc.getCriteriaBuilder().and(predicates) : qc.getCriteriaBuilder().or(predicates);
  }
}
