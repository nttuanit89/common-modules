package com.learning.common.crud.finding;

import com.learning.common.crud.condition.Condition;
import com.learning.common.crud.condition.Conditions;
import com.learning.common.crud.util.EntityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FindPageEntityHandler<T> extends AbstractFindEntityHandler<T> {
  private Pageable pageable;
  private Field fieldId;

  public FindPageEntityHandler(Class clazz, String[] fields, FindContext findContext) {
    super(clazz, fields, findContext);
  }

  @Override
  public FindPageEntityHandler<T> withCondition(Condition condition) {
    super.withCondition(condition);
    return this;
  }

  @Override
  public FindPageEntityHandler<T> withJoinType(JoinType joinType) {
    super.withJoinType(joinType);
    return this;
  }

  public FindPageEntityHandler<T> withPageable(Pageable pageInfo) {
    if (pageInfo != null) {
      this.pageable =
          PageRequest.of(
              pageInfo.getPageNumber(), pageInfo.getPageSize() > 0 ? pageInfo.getPageSize() : 10);
    }
    return this;
  }

  public FindPageEntityHandler<T> withPageable(int page, int size) {
    page = page < 0 ? 0 : page;
    size = size < 1 ? 10 : size;
    this.pageable = PageRequest.of(page, size);
    return this;
  }

  public Page<T> execute() {
    return PageableExecutionUtils.getPage(
        getListResult(), pageable, () -> (Long) createCountQuery().getSingleResult());
  }

  @Override
  public FindPageEntityHandler<T> sortBy(Sort... sort) {
    super.sortBy(sort);
    return this;
  }

  @Override
  public FindPageEntityHandler<T> setEmptyLazyFields() {
    super.setEmptyLazyFields();
    return this;
  }

  @Override
  protected Condition getMoreCriteria(QueryContext qc) {
    if (pageable == null) {
      pageable = PageRequest.of(0, 10);
    }

    Field idField =
        EntityUtils.getField(getEntityClass(), EntityUtils.getIdFieldName(getEntityClass()));
    List ids = getIdsInPage(idField, pageable.getPageNumber(), pageable.getPageSize());
    return Conditions.or(
        (Condition[])
            ids.stream()
                .map(id -> Condition.field(idField.getName()).equal(id))
                .toArray(len -> new Condition[len]));
  }

  @Override
  protected List<Path> addSortInfo(QueryContext qc, CriteriaQuery criteriaQuery) {
    if (getSorts() != null && getSorts().length > 0) {
      return addSortInfo(qc, criteriaQuery, getSorts());
    }
    return addSortInfo(qc, criteriaQuery, Sort.ascList(this.fieldId.getName()));
  }

  private List getIdsInPage(Field idField, int page, int size) {
    this.fieldId = idField;
    QueryContext qc = super.createCriteriaQuery(Tuple.class);
    Root root = qc.getRoot();
    if (getCondition() != null) {
      qc.getQuery().where(getCondition().toPredicate(qc));
    }
    List<Path> sortFields = new ArrayList<>(addSortInfo(qc, qc.getQuery()));
    sortFields.add(0, root.get(idField.getName()));
    qc.getQuery().multiselect(sortFields).distinct(true);
    Query query = getFindContext().getEntityManager().createQuery(qc.getQuery());
    query.setFirstResult(page * size);
    query.setMaxResults(size);
    return (List)
            query.getResultList().stream()
                .map(t -> ((Tuple) t).get(0))
                .collect(Collectors.toList());
  }

  private TypedQuery createCountQuery() {
    QueryContext qc = super.createCriteriaQuery(Long.class);
    Root root = qc.getRoot();
    qc.getQuery()
        .select(
            getFindContext()
                .getEntityManager()
                .getCriteriaBuilder()
                .countDistinct(root.get(this.fieldId.getName())));
    if (getCondition() != null) {
      qc.getQuery().where(getCondition().toPredicate(qc));
    }
    return getFindContext().getEntityManager().createQuery(qc.getQuery());
  }
}
