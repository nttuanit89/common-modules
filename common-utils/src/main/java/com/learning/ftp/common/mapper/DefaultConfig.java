package com.learning.ftp.common.mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import static org.mapstruct.ReportingPolicy.IGNORE;

/*
 * @deprecated Use {@link } @DefaultGrpcConfig instead.
 */
@Deprecated
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = IGNORE,
    injectionStrategy = CONSTRUCTOR,
    uses = {SimpleTypeMapper.class, GrpcTypeMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DefaultConfig {}
