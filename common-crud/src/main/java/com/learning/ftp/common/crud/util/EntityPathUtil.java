package com.learning.ftp.common.crud.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.learning.ftp.common.crud.finding.QueryContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import org.springframework.util.StringUtils;

public class EntityPathUtil {
  public static Path getFieldPath(Path root, String fieldName, JoinType... joinType) {
    String[] parts = fieldName.split("\\s*\\.\\s*");
    Path path = root;

    for (int i = 0; i < parts.length; ++i) {
      if (!EntityUtils.isEntityClass(path.get(parts[i]).getJavaType())
          && !(path.get(parts[i]) instanceof PluralAttributePath)) {
        path = path.get(parts[i]);
        continue;
      }
      if (path instanceof From) {
        path = ((From) path).join(parts[i], joinType.length == 0 ? JoinType.LEFT : joinType[0]);
      } else if (path instanceof Join) {
        path = ((Join) path).join(parts[i], joinType.length == 0 ? JoinType.LEFT : joinType[0]);
      }
    }

    return path;
  }

  public static Expression getFieldPath(QueryContext qc, String fieldName, JoinType... joinType) {
    if (isJsonField(fieldName)) {
      return getJsonFieldPath(qc, fieldName);
    }

    return getFieldPath(qc.getSelectionPaths(), qc.getRoot(), fieldName, joinType);
  }

  public static Path getFieldPath(
      Map<String, Path> selectionPaths, Path root, String fieldName, JoinType... joinType) {
    String selectionField =
        selectionPaths.keySet().stream()
            .filter(f -> isFieldNameContainsSelectionField(f, fieldName))
            .findFirst()
            .orElse(null);
    if (!StringUtils.isEmpty(selectionField)) {
      return getDeepPath(selectionPaths.get(selectionField), fieldName);
    }
    return getFieldPath(root, fieldName, joinType);
  }

  private static Path getDeepPath(Path path, String fieldName) {
    String orgFieldName = path.getAlias().replaceAll("(_p_)+$", "").replaceAll("__", ".");
    if (orgFieldName.equals(fieldName)) {
      return path;
    }
    return getFieldPath(path, fieldName.substring(orgFieldName.length() + 1));
  }

  private static boolean isFieldNameContainsSelectionField(String selField, String fieldName) {
    String[] selFieldParts = selField.split("\\s*\\.\\s*");
    String[] fieldNameParts = fieldName.split("\\s*\\.\\s*");

    if (fieldNameParts.length < selFieldParts.length) {
      return false;
    }
    for (int i = 0; i < selFieldParts.length; ++i) {
      if (!selFieldParts[i].equals(fieldNameParts[i])) {
        return false;
      }
    }
    return true;
  }

  public static boolean isJsonField(String fieldName) {
    return fieldName.contains("->");
  }

  public static Expression getJsonFieldPath(QueryContext qc, String fieldName) {
    String[] jsonPath = fieldName.replace(" ", "").split("->");

    List<Expression> literals = Lists.newArrayList(qc.getRoot().get(jsonPath[0]));
    List<Expression> pathLiterals =
        Arrays.stream(jsonPath)
            .skip(1)
            .map(path -> qc.getCriteriaBuilder().literal(path))
            .collect(Collectors.toList());

    literals.addAll(pathLiterals);
    Expression[] literalArray = Iterables.toArray(literals, Expression.class);
    return qc.getCriteriaBuilder().function("jsonb_extract_path_text", String.class, literalArray);
  }
}
