package com.learning.common.crud.finding;

import com.learning.common.crud.condition.Condition;
import com.learning.common.crud.util.EntityUtils;
import java.util.List;

import org.springframework.data.domain.Page;

public class FindOneEntityHandler<T> extends AbstractFindEntityHandler<T> {
  private FindPageEntityHandler findPageHandler;
  private boolean ensureMaxOneResult = false;

  public FindOneEntityHandler(Class clazz, String[] fields, FindContext findContext) {
    super(clazz, fields, findContext);
    findPageHandler = new FindPageEntityHandler(clazz, fields, findContext);
  }

  @Override
  public FindOneEntityHandler<T> withCondition(Condition condition) {
    super.withCondition(condition);
    findPageHandler.withCondition(condition);
    return this;
  }

  public T execute() {
    if (ensureMaxOneResult == false) {
      return executeFindOne();
    }
    List<T> resultList = getListResult();
    return !resultList.isEmpty() ? resultList.get(0) : null;
  }

  private T executeFindOne() {
    findPageHandler.withPageable(0, 1);
    Page<T> result = findPageHandler.execute();
    if (result.isEmpty()) {
      return null;
    }
    T t = result.getContent().get(0);
    if (EntityUtils.isObjectProxy(t)) {
      t = EntityUtils.unProxyEntity(t);
    }
    return t;
  }

  @Override
  public FindOneEntityHandler<T> sortBy(Sort... sort) {
    super.sortBy(sort);
    findPageHandler.sortBy(sort);
    return this;
  }

  public FindOneEntityHandler<T> sureMaxOneResult() {
    ensureMaxOneResult = true;
    return this;
  }
}
