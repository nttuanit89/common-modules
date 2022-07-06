package com.learning.common.crud.finding;

import com.learning.common.crud.condition.Condition;
import com.learning.common.crud.condition.Conditions;
import com.learning.common.exception.FtpServiceException;
import com.learning.common.crud.util.EntityPathUtil;
import com.learning.common.crud.util.EntityUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.MapKey;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.springframework.util.StringUtils;

public abstract class AbstractFindEntityHandler<T> {
  private static final String ROOT_ALIAS = "_root";
  private Condition condition;
  private Class<T> entityClass;
  private FindContext findContext;
  private String[] fields;
  private JoinType joinType = JoinType.LEFT;
  private Sort[] sorts;
  private boolean isEmptyLazyField = false;

  public AbstractFindEntityHandler(Class clazz, String[] fields, FindContext findContext) {
    this.entityClass = clazz;
    this.fields = createFields(fields);
    this.findContext = findContext;
  }

  private String[] createFields(String[] fields) {
    FieldPath root = new FieldPath();
    List<String> fieldPaths = new ArrayList<>();
    for (String s : Arrays.asList(fields)) {
      root.addChild(s);
    }
    root.traverse(p -> fieldPaths.add(p.getFullPath()));
    return fieldPaths.toArray(new String[] {});
  }

  protected AbstractFindEntityHandler<T> withCondition(Condition condition) {
    this.condition = condition;
    return this;
  }

  protected AbstractFindEntityHandler<T> withJoinType(JoinType joinType) {
    this.joinType = joinType;
    return this;
  }

  protected AbstractFindEntityHandler<T> sortBy(Sort... sort) {
    this.sorts = sort;
    return this;
  }

  protected List<T> getListResult() {
    QueryResult<T> qr = executeQuery();
    boolean hasMoreFields = fields.length > 0;
    if (!hasMoreFields) {
      return (List<T>)
          new LinkedHashSet(qr.getResult())
              .stream()
                  .map(e -> isEmptyLazyField ? fillNullToLazyFields(e) : e)
                  .collect(Collectors.toList());
    }
    Map<String, String> fieldToAlias =
        qr.getAliasMapping().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    return groupTupleByRoot((List<Tuple>) qr.getResult()).entrySet().stream()
        .map(e -> addFieldsToEntity(fieldToAlias, e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  private T addFieldsToEntity(Map<String, String> fieldToAlias, T entity, List<Tuple> tuples) {
    Map<String, Object> fieldValues = new HashMap<>();
    for (String fieldName : fields) {
      List values =
          tuples.stream()
              .map(t -> t.get(fieldToAlias.get(fieldName)))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      Object fieldValue;
      if (!fieldName.contains(".")) {
        fieldValue =
            setFieldValueToEntity(
                entity, values, EntityUtils.getField(entity.getClass(), fieldName));
      } else {
        int i = fieldName.lastIndexOf(".");
        String realFieldName = fieldName.substring(i + 1);
        String parentField = fieldName.substring(0, i);
        fieldValue = fieldValues.get(parentField);
        fieldValue = setFieldValueToObject(fieldValue, values, realFieldName);
      }
      fieldValues.put(fieldName, fieldValue);
    }
    return entity;
  }

  private Object setFieldValueToEntity(Object entity, List tupleElements, Field field) {
    Class fieldType = field.getType();
    if (!Map.class.isAssignableFrom(fieldType) && !Collection.class.isAssignableFrom(fieldType)) {
      if (tupleElements.isEmpty()) {
        return null;
      }
      EntityUtils.setFieldValue(entity, field, tupleElements.get(0));
      return tupleElements.get(0);
    }
    Object fieldValue = EntityUtils.getFieldValue(entity, field.getName());
    LinkedHashSet setItem = new LinkedHashSet(tupleElements);
    if (Map.class.isAssignableFrom(fieldType)) {
      HashMap map = new HashMap();
      MapKey mk = field.getAnnotation(MapKey.class);
      map.putAll(
          (Map)
              setItem.stream()
                  .collect(Collectors.toMap(e -> EntityUtils.getFieldValue(e, mk.name()), e -> e)));
      if (!shouldSetCollectionValueToEntity(map.keySet(), (Collection) fieldValue)) {
        return fieldValue;
      }
      EntityUtils.setFieldValue(entity, field, map);
      return map;
    }
    if (Set.class.isAssignableFrom(fieldType)) {
      Set set = new HashSet(setItem);
      if (!shouldSetCollectionValueToEntity(set, (Collection) fieldValue)) {
        return fieldValue;
      }
      EntityUtils.setFieldValue(entity, field, set);
      return set;
    }
    List list = new ArrayList((List) setItem.stream().collect(Collectors.toList()));
    if (!shouldSetCollectionValueToEntity(list, (Collection) fieldValue)) {
      return fieldValue;
    }
    EntityUtils.setFieldValue(entity, field, list);
    return list;
  }

  private static boolean shouldSetCollectionValueToEntity(
      Collection newValue, Collection currentValue) {
    try {
      if (!EntityUtils.isObjectProxy(currentValue)
          && currentValue != null
          && newValue.size() == currentValue.size()) {
        return false;
      }
    } catch (Throwable err) {
      return true;
    }
    return true;
  }

  private Object setFieldValueToObject(Object target, List tupleElements, String fieldName) {
    Class targetClass = target.getClass();
    if (EntityUtils.isEntity(target)) {
      return setFieldValueToEntity(
          target, tupleElements, EntityUtils.getField(targetClass, fieldName));
    }
    Collection<?> values;
    if (Map.class.isAssignableFrom(targetClass)) {
      values = ((Map) target).values();
    } else {
      values = (Collection) target;
    }
    List addedValues = new ArrayList();
    for (Object entityItem : values) {
      List fieldVal =
          (List)
              tupleElements.stream()
                  .filter(e -> isEntityParentOf(entityItem, e, fieldName))
                  .collect(Collectors.toList());
      if (fieldVal != null) {
        addedValues.add(
            setFieldValueToEntity(
                entityItem, fieldVal, EntityUtils.getField(entityItem.getClass(), fieldName)));
      }
    }
    return addedValues;
  }

  private boolean isEntityParentOf(Object entity, Object childEntity, String fieldName) {
    if (childEntity == null || entity == null) {
      return false;
    }
    Object fieldValue = EntityUtils.getFieldValue(entity, fieldName);
    if (!EntityUtils.isEntityClass(childEntity.getClass())) {
      return Objects.equals(fieldValue, childEntity);
    }
    if (Objects.equals(EntityUtils.getId(childEntity), EntityUtils.getId(fieldValue))) {
      return true;
    }
    return EntityUtils.getFields(childEntity.getClass(), entity.getClass()).keySet().stream()
        .filter(fn -> EntityUtils.getFieldValue(childEntity, fn).equals(entity))
        .findFirst()
        .isPresent();
  }

  private Map<T, List<Tuple>> groupTupleByRoot(List<Tuple> tuples) {
    LinkedHashMap<T, List<Tuple>> entityTuples = new LinkedHashMap<>();
    for (Tuple tuple : tuples) {
      List<Tuple> entityPart;
      T entityRoot = (T) tuple.get(ROOT_ALIAS);
      if (isEmptyLazyField) {
        fillNullToLazyFields(entityRoot);
      }
      if (!entityTuples.containsKey(entityRoot)) {
        entityPart = new ArrayList<>();
        entityTuples.put(entityRoot, entityPart);
      } else {
        entityPart = entityTuples.get(entityRoot);
      }
      entityPart.add(tuple);
    }
    return entityTuples;
  }

  private <T> T fillNullToLazyFields(T entity) {
    // fill null all lazy fields
    EntityUtils.fill(entity, EntityUtils.getLazyEntityFields(entity.getClass()), null);
    return entity;
  }

  private QueryResult<T> executeQuery() {
    QueryContext qc = createCriteriaQuery();
    try {
      Condition moreCondition = getMoreCriteria(qc);
      CriteriaQuery criteriaQuery = qc.getQuery();
      if (condition != null && moreCondition == null) {
        criteriaQuery.where(condition.toPredicate(qc));
      } else if (condition == null && moreCondition != null) {
        criteriaQuery.where(moreCondition.toPredicate(qc));
      } else if (condition != null && moreCondition != null) {
        criteriaQuery.where(Conditions.and(condition, moreCondition).toPredicate(qc));
      }
      addSortInfo(qc, criteriaQuery);
      Query sqlQuery = findContext.getEntityManager().createQuery(criteriaQuery);
      setResultSizeAndSegment(sqlQuery);
      return new QueryResult<>(qc.getAliasMapping(), sqlQuery.getResultList());
    } catch (Exception ex) {
      throw new FtpServiceException("Error when executeQuery: " + ex.getMessage(), ex);
    }
  }

  protected List<Path> addSortInfo(QueryContext qc, CriteriaQuery criteriaQuery) {
    return addSortInfo(qc, criteriaQuery, sorts);
  }

  protected AbstractFindEntityHandler setEmptyLazyFields() {
    this.isEmptyLazyField = true;
    return this;
  }

  protected List<Path> addSortInfo(QueryContext qc, CriteriaQuery criteriaQuery, Sort[] sorts) {
    if (sorts == null || sorts.length < 1) {
      return Collections.emptyList();
    }
    Map<Expression, Boolean> sortFields = new LinkedHashMap<>();
    Arrays.asList(sorts).stream()
        .filter(Objects::nonNull)
        .forEach(
            s -> {
              Expression path = s.toExpression(qc);
              sortFields.put(path, s.isAsc());
            });
    criteriaQuery.orderBy(
        sortFields.entrySet().stream()
            .map(
                e ->
                    e.getValue()
                        ? qc.getCriteriaBuilder().asc(e.getKey())
                        : qc.getCriteriaBuilder().desc(e.getKey()))
            .collect(Collectors.toList()));

    return new ArrayList(sortFields.keySet());
  }

  protected Sort[] getSorts() {
    return this.sorts;
  }

  protected void setResultSizeAndSegment(Query sqlQuery) {}

  protected QueryContext createCriteriaQuery(Class<?>... resultClass) {
    EntityManager em = findContext.getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    boolean hasMoreFields = fields.length > 0;
    CriteriaQuery query;
    Map<String, Path> selectionPaths = new HashMap<>();
    Map<String, String> aliasMapping = new HashMap<>();
    if (resultClass.length == 0) {
      query = hasMoreFields ? cb.createTupleQuery() : cb.createQuery(entityClass);
    } else {
      query =
          resultClass[0] == Tuple.class ? cb.createTupleQuery() : cb.createQuery(resultClass[0]);
    }
    Root root = query.from(entityClass);
    if (hasMoreFields) {
      query.multiselect(getSelections(root, selectionPaths, aliasMapping));
    } else {
      query.select(root);
    }

    return new QueryContext(entityClass, root, cb, query, selectionPaths, aliasMapping);
  }

  protected Condition getMoreCriteria(QueryContext qc) {
    return null;
  }

  private Selection[] getSelections(
      Root<T> root, Map<String, Path> selectionPaths, Map<String, String> aliasMapping) {
    List<Selection> paths = new ArrayList<>();

    paths.add(createSelectionAlias(root, ROOT_ALIAS, aliasMapping));
    paths.addAll(
        Arrays.asList(fields).stream()
            .map(
                f -> {
                  Path selPath = EntityPathUtil.getFieldPath(selectionPaths, root, f, joinType);
                  selectionPaths.put(f, selPath);
                  return createSelectionAlias(selPath, f, aliasMapping);
                })
            .collect(Collectors.toList()));
    return paths.toArray(new Path[] {});
  }

  protected Selection createSelectionAlias(
      Path path, String fieldName, Map<String, String> aliasMapping) {
    String alias = fieldName.replaceAll("\\.", "__");
    if (StringUtils.isEmpty(alias)) {
      alias = "_p_";
    }
    while (aliasMapping.containsKey(alias)) {
      alias = alias + "_p_";
    }
    if (!ROOT_ALIAS.equals(alias) && !alias.endsWith("_p_")) {
      alias = alias + "_p_";
    }
    aliasMapping.put(alias, fieldName);
    return path.alias(alias);
  }

  public FindContext getFindContext() {
    return findContext;
  }

  public Class<T> getEntityClass() {
    return entityClass;
  }

  public Condition getCondition() {
    return condition;
  }
}
