package com.learning.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for dealing with Json.
 *
 * @author Thuan Xinh
 */
public class JsonMapper {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Get the mapper.
   *
   * @return the mapper
   */
  public static ObjectMapper getMapper() {
    return mapper;
  }

  /**
   * Deserialize a json string into java object.
   *
   * @param jsonString json string
   * @param object target object
   * @param <T> object type
   * @return deserialized object
   */
  public static <T> T toObject(String jsonString, Class<T> object) {
    try {
      return mapper.readValue(jsonString, object);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Deserialize object to a another object.
   *
   * @param object object data
   * @param tClass target java type
   * @param <T> object type
   * @return deserialized object
   */
  public static <T> T convertValue(Object object, Class<T> tClass) {
    try {
      return mapper.convertValue(object, tClass);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Deserialize object to a another object.
   *
   * @param object object data
   * @param typeReference typeReference
   * @param <T> object type
   * @return deserialized object
   */
  public static <T> T convertValue(Object object, TypeReference<T> typeReference) {
    try {
      return mapper.convertValue(object, typeReference);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Deserialize a json string into java object.
   *
   * @param jsonString json string
   * @param javaType target java type
   * @param <T> object type
   * @return deserialized object
   */
  public static <T> T toObject(String jsonString, JavaType javaType) {
    try {
      return mapper.readValue(jsonString, javaType);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Deserialize a json string into java object.
   *
   * @param jsonString json string
   * @param typeReference target type reference
   * @param <T> object type
   * @return deserialized object
   */
  public static <T> T toObject(String jsonString, TypeReference<T> typeReference) {
    try {
      return mapper.readValue(jsonString, typeReference);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Serialize an java object into json string.
   *
   * @param object source object
   * @return serialized json string
   */
  public static String toString(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
