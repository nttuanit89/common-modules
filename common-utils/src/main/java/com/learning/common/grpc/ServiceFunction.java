package com.learning.common.grpc;

import com.google.protobuf.Message;

@FunctionalInterface
public interface ServiceFunction<T extends Message, V> {
  V call(T t) throws Exception;
}
