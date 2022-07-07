package com.learning.ftp.common.crud.finding;

import java.util.List;
import java.util.Map;

public class QueryResult<T> {
  private Map<String, String> aliasMapping;
  private List<T> result;

  public QueryResult(Map<String, String> aliasMapping, List<T> result) {
    this.aliasMapping = aliasMapping;
    this.result = result;
  }

  public Map<String, String> getAliasMapping() {
    return aliasMapping;
  }

  public List<T> getResult() {
    return result;
  }
}
