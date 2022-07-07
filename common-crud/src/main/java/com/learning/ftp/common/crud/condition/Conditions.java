package com.learning.ftp.common.crud.condition;

public class Conditions {
  public static Condition and(Condition... conditions) {
    return new CompositeCondition(true, conditions);
  }

  public static Condition or(Condition... conditions) {
    return new CompositeCondition(false, conditions);
  }
}
