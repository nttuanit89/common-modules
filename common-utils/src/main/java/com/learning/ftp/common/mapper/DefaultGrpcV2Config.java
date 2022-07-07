package com.learning.ftp.common.mapper;

import com.google.protobuf.GeneratedMessageV3;
import static org.mapstruct.CollectionMappingStrategy.ADDER_PREFERRED;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import static org.mapstruct.MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import static org.mapstruct.ReportingPolicy.IGNORE;

@SuppressWarnings("rawtypes")
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = IGNORE,
    injectionStrategy = CONSTRUCTOR,
    uses = {SimpleTypeMapper.class, GrpcTypeMapper.class, ByteStringMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    collectionMappingStrategy = ADDER_PREFERRED,
    mappingInheritanceStrategy = AUTO_INHERIT_FROM_CONFIG)
public interface DefaultGrpcV2Config {

  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  GeneratedMessageV3 toGrpc(GeneratedMessageV3 any);

  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  GeneratedMessageV3.Builder toGrpcBuilder(GeneratedMessageV3 any);
}
