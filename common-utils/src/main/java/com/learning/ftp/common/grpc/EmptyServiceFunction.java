package com.learning.ftp.common.grpc;

import com.google.protobuf.Message;

@FunctionalInterface
public interface EmptyServiceFunction<T extends Message> {
  void call(T t) throws Exception;
}
