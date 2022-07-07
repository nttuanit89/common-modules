package com.learning.ftp.common.crud.finding;

import javax.persistence.EntityManager;

public interface FindContext {
  EntityManager getEntityManager();
}
