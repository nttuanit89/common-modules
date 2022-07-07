package com.learning.ftp.common.mapper;

import com.google.protobuf.ByteString;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ByteStringMapper {
  default String mapToString(ByteString byteString) {
    return byteString.toStringUtf8();
  }

  default ByteString mapToByteString(String str) {
    return ByteString.copyFromUtf8(str);
  }
}
