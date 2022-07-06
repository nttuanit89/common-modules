package com.learning.common.crud.finding;

import javax.persistence.EntityManager;

public interface FindContext {
  EntityManager getEntityManager();
}
