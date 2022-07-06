package com.learning.common.crud.finding;

import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

public class QueryContext<T> {
  private CriteriaQuery query;
  private CriteriaBuilder criteriaBuilder;
  private Root root;
  private Class entityClass;
  private Map<String, Path> selectionPaths;
  private Map<String, String> aliasMapping;

  public QueryContext(
      Class entityClass,
      Root root,
      CriteriaBuilder cb,
      CriteriaQuery query,
      Map<String, Path> selectionPaths,
      Map<String, String> aliasMapping) {
    this.entityClass = entityClass;
    this.root = root;
    this.criteriaBuilder = cb;
    this.selectionPaths = selectionPaths;
    this.query = query;
    this.aliasMapping = aliasMapping;
  }

  public CriteriaBuilder getCriteriaBuilder() {
    return criteriaBuilder;
  }

  public Root getRoot() {
    return root;
  }

  public Class getEntityClass() {
    return entityClass;
  }

  public Map<String, Path> getSelectionPaths() {
    return selectionPaths;
  }

  public CriteriaQuery getQuery() {
    return query;
  }

  public Map<String, String> getAliasMapping() {
    return aliasMapping;
  }
}
