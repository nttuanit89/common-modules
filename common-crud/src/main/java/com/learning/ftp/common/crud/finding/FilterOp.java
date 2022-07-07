package com.learning.ftp.common.crud.finding;

public enum FilterOp {
  Like("like"),
  Equals("equal"),
  In("in"),
  IsNull("isNull"),
  IsNotNull("isNotNull"),
  GreaterThan("greaterThan"),
  LessThan("lessThan"),
  Between("between");

  private String op;

  private FilterOp(String opName) {
    this.op = opName;
  }

  public String getOp() {
    return op;
  }
}
