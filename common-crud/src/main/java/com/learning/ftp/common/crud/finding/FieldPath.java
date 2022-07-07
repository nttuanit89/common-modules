package com.learning.ftp.common.crud.finding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.util.StringUtils;

public class FieldPath {
  private List<FieldPath> childPaths;
  private String fieldName;
  private FieldPath parent;
  private String fullPath;

  public FieldPath() {
    childPaths = new ArrayList<>();
    fieldName = "";
    fullPath = "";
  }

  private FieldPath(String name) {
    this();
    fieldName = name;
  }

  public void addChild(FieldPath cfp) {
    if (cfp == null) {
      return;
    }
    childPaths.add(cfp);
    cfp.parent = this;
    cfp.fullPath =
        StringUtils.isEmpty(fullPath)
            ? cfp.fieldName
            : String.format("%s.%s", fullPath, cfp.fieldName);
  }

  public void addChild(String fieldName) {
    if (StringUtils.isEmpty(fieldName)) {
      return;
    }
    FieldPath parent;
    if (fieldName.contains(".")) {
      int i = fieldName.lastIndexOf('.');
      parent = getFieldPathFrom(fieldName.substring(0, i));
      if (parent != null) {
        parent.addChild(fieldName.substring(i + 1));
        return;
      }
      addChildren(fieldName);
    } else {
      addChild(new FieldPath(fieldName));
    }
  }

  private void addChildren(String fieldPath) {
    int i = fieldPath.indexOf(".");
    String firstPart = i >= 0 ? fieldPath.substring(0, i) : fieldPath;
    String restPart = i >= 0 ? fieldPath.substring(i + 1) : "";
    FieldPath parent = getFieldPathFrom(firstPart);
    if (parent == null) {
      FieldPath fp = new FieldPath(firstPart);
      addChild(fp);
      parent = fp;
    }
    if (!StringUtils.isEmpty(restPart)) {
      parent.addChildren(restPart);
    }
  }

  public FieldPath getFieldPathFrom(String fieldPath) {
    if (!fieldPath.contains(".")) {
      return childPaths.stream()
          .filter(p -> p.fieldName.equals(fieldPath))
          .findFirst()
          .orElse(null);
    }
    String[] parts = fieldPath.split("\\s*\\.\\s*");
    FieldPath parent = this;
    for (String part : parts) {
      parent = parent.getFieldPathFrom(part);
      if (parent == null) {
        return null;
      }
    }
    return parent;
  }

  public void traverse(Consumer<FieldPath> consumer) {
    if (!StringUtils.isEmpty(fieldName)) {
      consumer.accept(this);
    }
    childPaths.stream().forEach(p -> p.traverse(consumer));
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getFullPath() {
    return fullPath;
  }
}
