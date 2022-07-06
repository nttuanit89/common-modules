package com.learning.common.mapper;

import com.google.protobuf.GeneratedMessageV3;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import static org.mapstruct.CollectionMappingStrategy.ADDER_PREFERRED;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG;
import static org.mapstruct.ReportingPolicy.IGNORE;

/*
 * @deprecated Use {@link } @DefaultGrpcV2Config instead.
 */
@Deprecated
@SuppressWarnings("rawtypes")
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = IGNORE,
    injectionStrategy = CONSTRUCTOR,
    uses = {SimpleTypeMapper.class, GrpcTypeMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    collectionMappingStrategy = ADDER_PREFERRED,
    mappingInheritanceStrategy = AUTO_INHERIT_FROM_CONFIG)
public interface DefaultGrpcConfig {

  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  GeneratedMessageV3 toGrpc(GeneratedMessageV3 any);

  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  GeneratedMessageV3.Builder toGrpcBuilder(GeneratedMessageV3 any);
}
