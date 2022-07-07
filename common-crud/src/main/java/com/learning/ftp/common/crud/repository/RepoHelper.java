package com.learning.ftp.common.crud.repository;

import com.learning.ftp.common.crud.repository.specification.FbSpec.FbCustomSpec;
import com.learning.ftp.common.crud.util.TransactionHelper;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.RootEntityResultTransformer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Component;

@SuppressWarnings({"rawtypes", "unchecked", "unused"})
@Component
@Lazy
public class RepoHelper {
  private final Repositories repositories;
  private final TransactionHelper helper;
  @Getter private final EntityManager em;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  RepoHelper(
      ApplicationContext context,
      Optional<List<IBaseRepository>> repos,
      EntityManager em,
      TransactionHelper helper) {
    repositories = new Repositories(context);
    this.em = em;
    this.helper = helper;
  }

  public <T> EntityGraph<T> createGraph(Class<T> clazz, String... paths) {
    EntityGraph<T> graph = em.createEntityGraph(clazz);
    Arrays.stream(paths).forEach(graph::addAttributeNodes);
    return graph;
  }

  public <T, ID extends Serializable> IBaseRepository<T, ID> get(Class<T> clazz) {
    return (IBaseRepository) repositories.getRepositoryFor(clazz).orElseThrow();
  }

  public <T> List<T> nativeQuery(String query, Class<T> resultClass) {
    return nativeQuery(QueryDto.<T>builder().query(query).resultClass(resultClass).build());
  }

  public <T> List<T> nativeQuery(String query, Class<T> resultClass, Consumer<NativeQuery> config) {
    return nativeQuery(
        QueryDto.<T>builder().query(query).resultClass(resultClass).config(config).build());
  }

  public <T> List<T> nativeQuery(QueryDto<T> dto) {
    return ofNullable(dto.getEagerLoads())
        .filter(Predicate.not(List::isEmpty))
        .map(config -> helper.doInTransaction(() -> queryAndFetch(dto)))
        .orElseGet(() -> queryAndFetch(dto));
  }

  @SuppressWarnings("deprecation")
  private <T> List<T> queryAndFetch(QueryDto<T> dto) {
    NativeQuery query =
        (dto.getResultClass().isAnnotationPresent(Entity.class)
                ? em.createNativeQuery(dto.getQuery(), dto.getResultClass())
                : em.createNativeQuery(dto.getQuery()))
            .unwrap(NativeQuery.class);
    dto.getConfigs().forEach(config -> config.accept(query));
    query.setResultTransformer(dto.getTransformer());
    List<T> result = query.getResultList();
    if (!dto.getEagerLoads().isEmpty()) {
      result.forEach(e -> dto.getEagerLoads().forEach(el -> el.accept(e)));
    }
    return result;
  }

  public <T> javax.persistence.criteria.Predicate toPredicate(
      Stream.Builder<FbCustomSpec<T>> streamBuilder,
      From<T, ?> attributes,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder) {
    javax.persistence.criteria.Predicate[] restrictions =
        streamBuilder
            .build()
            .map(cond -> cond.toFbPredicate(attributes, query, criteriaBuilder))
            .filter(Objects::nonNull)
            .toArray(javax.persistence.criteria.Predicate[]::new);
    return em.getCriteriaBuilder().and(restrictions);
  }

  @Value
  @Builder
  public static class QueryDto<T> {
    @NonNull String query;
    @NonNull Class<T> resultClass;
    @Default ResultTransformer transformer = RootEntityResultTransformer.INSTANCE;
    @Singular List<Consumer<NativeQuery>> configs;
    @Singular List<Consumer<T>> eagerLoads;
  }
}
