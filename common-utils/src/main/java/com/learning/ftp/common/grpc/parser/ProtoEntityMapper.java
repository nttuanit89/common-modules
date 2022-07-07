package com.learning.ftp.common.grpc.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtoEntityMapper {

  private static ObjectMapper mapper = new ObjectMapper();

  public static <T extends Message, U> U toEntity(T source, Class<U> desc) {
    try {
      String json =
          JsonFormat.printer()
              .includingDefaultValueFields()
              .omittingInsignificantWhitespace()
              .print(source);
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      U obj = mapper.readValue(json, desc);
      return obj;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T extends Message, U> U toEntity(T source, TypeReference<U> valueTypeRef) {
    try {
      String json =
          JsonFormat.printer()
              .includingDefaultValueFields()
              .omittingInsignificantWhitespace()
              .print(source);
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      return mapper.readValue(json, valueTypeRef);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T, U extends GeneratedMessageV3.Builder> U toProto(T source, U desc) {
    try {
      String json = mapper.writeValueAsString(source);
      JsonFormat.parser().ignoringUnknownFields().merge(json, desc);
      return desc;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T extends GeneratedMessageV3.Builder> T toProto(String json, T protoBuilder) {
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(json, protoBuilder);
      return protoBuilder;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T extends Message, U extends GeneratedMessageV3.Builder> U toBuilder(
      T source, U desc) {
    try {
      String json =
          JsonFormat.printer()
              .omittingInsignificantWhitespace()
              .includingDefaultValueFields()
              .print(source);
      JsonFormat.parser().ignoringUnknownFields().merge(json, desc);
      return desc;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T extends Message> String toString(T message) {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Map<String, Value> toMapValue(Map m) {
    return ((Map<String, Object>) m)
        .entrySet().stream()
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> toValue(e.getValue())));
  }

  public static Struct toStruct(Object o) {
    if (o instanceof Map) {
      return Struct.newBuilder().putAllFields(toMapValue((Map) o)).build();
    }
    return toStruct(mapper.convertValue(o, Map.class));
  }

  public static Value toValue(Object o) {
    if (o instanceof String) {
      return Value.newBuilder().setStringValue((String) o).build();
    }
    if (o instanceof Number) {
      return Value.newBuilder().setNumberValue(Double.parseDouble(o.toString())).build();
    }
    if (o instanceof Boolean) {
      return Value.newBuilder().setBoolValue((boolean) o).build();
    }
    if (o instanceof Map) {
      return Value.newBuilder()
          .setStructValue(Struct.newBuilder().putAllFields(toMapValue((Map) o)).build())
          .build();
    }
    if (o instanceof List) {
      return Value.newBuilder()
          .setListValue(
              ListValue.newBuilder()
                  .addAllValues(
                      (List<Value>)
                          ((List) o).stream().map(i -> toValue(i)).collect(Collectors.toList()))
                  .build())
          .build();
    }
    return toValue(mapper.convertValue(o, Map.class));
  }
}
