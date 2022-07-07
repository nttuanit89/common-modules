package com.learning.ftp.common.crud.configuration;

import com.learning.ftp.common.crud.repository.IBaseRepository;
import com.learning.ftp.common.crud.repository.specification.FbSpec;
import java.io.Serializable;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import static org.hibernate.jpa.QueryHints.HINT_FETCHGRAPH;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;

@SuppressWarnings("NullableProblems")
class BaseRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
    implements IBaseRepository<T, ID> {
  protected final EntityManager entityManager;

  BaseRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.entityManager = entityManager;
  }

  @Override
  protected <S extends T> TypedQuery<S> getQuery(
      @Nullable Specification<S> spec, Class<S> domainClass, Sort sort) {
    TypedQuery<S> query = super.getQuery(spec, domainClass, sort);
    Optional.ofNullable(spec)
        .filter(FbSpec.class::isInstance)
        .map(FbSpec.class::cast)
        .map(FbSpec::getGraph)
        .ifPresent(graph -> query.setHint(HINT_FETCHGRAPH, graph));
    return query;
  }
}