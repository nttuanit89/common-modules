package com.learning.ftp.common.mapper;

import com.google.protobuf.Struct;
import com.learning.ftp.common.grpc.parser.ProtoEntityMapper;
import java.util.Map;
import java.util.Optional;
import org.mapstruct.Mapper;

@SuppressWarnings("unused")
@Mapper(componentModel = "spring")
public interface GrpcTypeMapper {

  default Map<String, Object> map(Struct value) {
    return ProtoEntityMapper.toEntity(value, Map.class);
  }

  default Struct map(Object o) {
    return Optional.ofNullable(o).map(ProtoEntityMapper::toStruct).orElse(null);
  }
}
