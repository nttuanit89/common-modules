package com.learning.ftp.common.crud;

import com.learning.ftp.common.crud.condition.Condition;
import com.learning.ftp.common.crud.finding.FindAllEntityHandler;
import com.learning.ftp.common.crud.finding.FindContext;
import com.learning.ftp.common.crud.finding.FindOneEntityHandler;
import com.learning.ftp.common.crud.finding.FindPageEntityHandler;
import com.learning.ftp.common.crud.util.EntityUtils;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toMap;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.Hibernate;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.Type;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import com.learning.ftp.common.util.UUIDUtils;

@SuppressWarnings({
  "rawtypes",
  "unchecked",
  "unused",
  "UnusedReturnValue",
  "deprecation",
  "OptionalUsedAsFieldOrParameterType"
})
@Deprecated
@Service
public class RepoService implements FindContext {
  private final Map<String, PagingAndSortingRepository> repositories;
  private final EntityManager entityManager;

  RepoService(Optional<List<PagingAndSortingRepository>> repos, EntityManager entityManager) {
    this.entityManager = entityManager;
    this.repositories =
        repos.stream()
            .flatMap(List::stream)
            .collect(
                toMap(
                    repo ->
                        ((ParameterizedType)
                                repo.getClass().getInterfaces()[0].getGenericInterfaces()[0])
                            .getActualTypeArguments()[0].getTypeName(),
                    repo -> repo));
  }

  public <T> Collection<T> executeQuery(String query, Map<String, Type>... mapDataTypes) {
    return executeParameterizedQuery(query, Collections.emptyMap(), mapDataTypes);
  }

  public <T> Collection<T> executeParameterizedQuery(
      String query, Map<String, Object> parameters, Map<String, Type>... mapDataTypes) {
    Query nativeQuery = getEntityManager().createNativeQuery(query);
    parameters.forEach(nativeQuery::setParameter);
    if (mapDataTypes.length > 0) {
      mapDataTypes[0].forEach(
          (key, value) -> nativeQuery.unwrap(NativeQuery.class).addScalar(key, value));
    }
    return nativeQuery.getResultList();
  }

  public <T> Page<T> executeQueryWithPaging(
      int page, int size, String query, Map<String, Type>... mapDataTypes) {
    String countQuery =
        Pattern.compile("select\\s+\\S+\\s+from\\s+(.+)", Pattern.CASE_INSENSITIVE)
            .matcher(query)
            .replaceAll("select count(*) from $1");
    return executeQueryWithPaging(page, size, query, countQuery, mapDataTypes);
  }

  public <T> Page<T> executeParameterizedQueryWithPaging(
      int page,
      int size,
      String query,
      Map<String, Object> parameters,
      Map<String, Type>... mapDataTypes) {
    String countQuery =
        Pattern.compile("select\\s+\\S+\\s+from\\s+(.+)", Pattern.CASE_INSENSITIVE)
            .matcher(query)
            .replaceAll("select count(*) from $1");
    return executeParameterizedQueryWithPaging(
        page, size, query, countQuery, parameters, mapDataTypes);
  }

  public <T> Page<T> executeQueryWithPaging(
      int page, int size, String query, String countQuery, Map<String, Type>... mapDataTypes) {
    page = Math.max(page, 0);
    size = size < 1 ? 10 : size;
    NativeQuery nativeQuery = getEntityManager().createNativeQuery(query).unwrap(NativeQuery.class);
    if (mapDataTypes.length > 0) {
      mapDataTypes[0].forEach(nativeQuery::addScalar);
    }
    return PageableExecutionUtils.getPage(
        nativeQuery.setFirstResult(page * size).setMaxResults(size).getResultList(),
        PageRequest.of(page, size),
        () ->
            ((BigInteger) getEntityManager().createNativeQuery(countQuery).getSingleResult())
                .longValue());
  }

  public <T> Page<T> executeParameterizedQueryWithPaging(
      int page,
      int size,
      String query,
      String countQuery,
      Map<String, Object> parameters,
      Map<String, Type>... mapDataTypes) {
    page = Math.max(page, 0);
    size = size < 1 ? 10 : size;
    Query nativeQuery = getEntityManager().createNativeQuery(query);
    Query countNativeQuery = getEntityManager().createNativeQuery(countQuery);
    parameters.forEach(
        (key, value) -> {
          nativeQuery.setParameter(key, value);
          countNativeQuery.setParameter(key, value);
        });
    if (mapDataTypes.length > 0) {
      mapDataTypes[0].forEach(nativeQuery.unwrap(NativeQuery.class)::addScalar);
    }
    return PageableExecutionUtils.getPage(
        nativeQuery.setFirstResult(page * size).setMaxResults(size).getResultList(),
        PageRequest.of(page, size),
        () -> ((BigInteger) countNativeQuery.getSingleResult()).longValue());
  }

  public <T> T findById(Class<T> entityType, Object id, String... fields) {
    if (Objects.isNull(id)) {
      return null;
    }

    if (fields != null && fields.length > 0) {
      return new FindOneEntityHandler<T>(entityType, fields, this)
          .sureMaxOneResult()
          .withCondition(Condition.field(EntityUtils.getIdFieldName(entityType)).equal(id))
          .execute();
    }

    Object identity = id;
    if (id instanceof String) {
      identity = UUIDUtils.fromString((String) id).map(Object.class::cast).orElse(id);
    }

    return (T) getAppropriateRepository(entityType).findById(identity).orElse(null);
  }

  public <T> T findEntity(Class<T> entityType, Object id) {
    return findById(entityType, id);
  }

  public <T> FindOneEntityHandler<T> findOne(Class<T> entityType, String... fields) {
    fields = fields != null ? fields : new String[] {};
    return new FindOneEntityHandler<>(entityType, fields, this);
  }

  public <T> FindAllEntityHandler<T> findAll(Class<T> entityType, String... fields) {
    fields = fields != null ? fields : new String[] {};
    return new FindAllEntityHandler<>(entityType, fields, this);
  }

  public <T> FindPageEntityHandler<T> findPage(Class<T> entityType, String... fields) {
    fields = fields != null ? fields : new String[] {};
    return new FindPageEntityHandler<>(entityType, fields, this);
  }

  public void deleteById(Class entityClazz, Object id) {
    if (!repositories.containsKey(entityClazz.getName())) {
      throw new RuntimeException(
          String.format(
              "Cannot find appropriate repository of entity %s", entityClazz.getSimpleName()));
    }
    repositories.get(entityClazz.getName()).deleteById(id);
  }

  public void delete(Object entity) {
    deleteById(entity.getClass(), EntityUtils.getId(entity));
  }

  public <T> void deleteBy(Class<T> entityClazz, String fieldName, Object value) {
    List<T> entities =
        findAll(entityClazz).withCondition(Condition.field(fieldName).equal(value)).execute();
    deleteAll(entities);
  }

  public <T> void deleteAll(List<T> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    if (!repositories.containsKey(entities.get(0).getClass().getName())) {
      throw new RuntimeException(
          String.format(
              "Cannot find appropriate repository of entity %s",
              entities.get(0).getClass().getSimpleName()));
    }
    repositories.get(entities.get(0).getClass().getName()).deleteAll(entities);
  }

  public <T> Iterable<T> insert(List<T> records) {
    if (records == null || records.isEmpty()) {
      return records;
    }
    PagingAndSortingRepository crudRepository = getAppropriateRepository(records.get(0));
    return crudRepository.saveAll(records);
  }

  public <T> T insert(T entity) {
    return (T) getAppropriateRepository(entity).save(entity);
  }

  public <T> T update(T entity, Function<T, T>... updateEntity) {
    return insert(entity);
  }

  public <T> List<T> update(List<T> entities) {
    if (entities == null || entities.isEmpty()) {
      return Collections.emptyList();
    }
    return (List<T>) getAppropriateRepository(entities.get(0)).saveAll(entities);
  }

  @Override
  public EntityManager getEntityManager() {
    return entityManager;
  }

  protected PagingAndSortingRepository getAppropriateRepository(Object data) {
    if (data instanceof Class) {
      return getCrudRepositoryFromEntityClass((Class) data);
    }
    return getCrudRepositoryFromData(data);
  }

  public PagingAndSortingRepository getCrudRepositoryFromEntityClass(Class clsType) {
    if (!repositories.containsKey(clsType.getName())) {
      throw new RuntimeException(
          String.format(
              "Cannot find appropriate repository of entity %s", clsType.getSimpleName()));
    }
    return repositories.get(clsType.getName());
  }

  private PagingAndSortingRepository getCrudRepositoryFromData(Object data) {
    if (data == null) {
      throw new RuntimeException("Entity data must not be null reference");
    }
    Class<?> clazz = AopUtils.getTargetClass(Hibernate.unproxy(data));
    if (!EntityUtils.isEntity(data)) {
      throw new RuntimeException(String.format("%s is not an entity", clazz.getSimpleName()));
    }

    if (!repositories.containsKey(clazz.getName())) {
      throw new RuntimeException(
          String.format("Cannot find appropriate repository of entity %s", clazz.getSimpleName()));
    }
    return repositories.get(clazz.getName());
  }
}
