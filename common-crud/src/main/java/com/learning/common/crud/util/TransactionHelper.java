package com.learning.common.crud.util;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
@Transactional
public class TransactionHelper {
  public <T> T doInTransaction(Supplier<T> supplier) {
    return supplier.get();
  }

  public void doInTransaction(Runnable runable) {
    runable.run();
  }
}
