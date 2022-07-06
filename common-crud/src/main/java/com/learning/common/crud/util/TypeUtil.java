package com.learning.common.crud.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

public class TypeUtil {
  public static TypeName extractClassName(Element e) {
    return ClassName.get(e.asType());
  }

  public static String getPackageName(Element e) {
    return getPackageName(getDrawTypeMirror(e));
  }

  public static String getParentPackageName(Element e) {
    String[] parts = getDrawTypeMirror(e).split("\\.");
    parts = Arrays.copyOfRange(parts, 0, parts.length - 2);
    return String.join(".", parts);
  }

  public static TypeName getTypeName(String fullName, String... paramTypes) {
    if (!fullName.contains(".")) {
      return getPrimitiveTypeName(fullName);
    }
    ClassName className = ClassName.get(getPackageName(fullName), getSimpleClassname(fullName));
    if (paramTypes.length == 0) {
      return className;
    }
    return ParameterizedTypeName.get(
        className,
        Arrays.asList(paramTypes).stream()
            .map(TypeUtil::getTypeName)
            .toArray(size -> new TypeName[size]));
  }

  public static String getPackageName(String fullName) {
    int dotPosition = fullName.lastIndexOf(".");
    return fullName.substring(0, dotPosition);
  }

  public static String getSimpleClassname(Element e) {
    return getSimpleClassname(getDrawTypeMirror(e));
  }

  public static String getSimpleClassname(String fullyName) {
    int lastDotPs = fullyName.lastIndexOf(".");
    return fullyName.substring(lastDotPs + 1);
  }

  private static String getDrawTypeMirror(Element e) {
    TypeMirror typeMirror = e.asType();
    return typeMirror.toString();
  }

  public static TypeName getPrimitiveTypeName(String type) {
    switch (type) {
      case "boolean":
        return ClassName.get("java.lang", "Boolean");
      case "short":
        return ClassName.get("java.lang", "Short");
      case "int":
        return ClassName.get("java.lang", "Integer");
      case "long":
        return ClassName.get("java.lang", "Long");
      case "char":
        return ClassName.get("java.lang", "Character");
      case "float":
        return ClassName.get("java.lang", "Float");
      case "double":
        return ClassName.get("java.lang", "Double");
    }
    return null;
  }
}
