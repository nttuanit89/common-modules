package com.learning.common.crud.util;

import com.learning.common.util.UUIDUtils;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityUtils {
  private static final Logger LOG = LoggerFactory.getLogger(EntityUtils.class);
  private static final Map<Class, Map<String, Field>> clazzFields = new HashMap<>();

  public static void trim(Object entity) {
    for (Field f : entity.getClass().getDeclaredFields()) {
      try {
        ReflectionUtils.makeAccessible(f);
        if ((f.getType() != String.class && f.getType() != StringBuilder.class)
            || f.get(entity) == null) {
          continue;
        }
        Object fVal = f.get(entity);
        f.set(entity, fVal.toString().trim());
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  public static <T> T fill(T entity, Map<String, ?> values) {
    for (Field f : entity.getClass().getDeclaredFields()) {
      try {
        ReflectionUtils.makeAccessible(f);
        if (!values.containsKey(f.getName())
            || values.get(f.getName()) == null
            || !f.getType().isPrimitive()
                && !f.getType().isAssignableFrom(values.get(f.getName()).getClass())) {
          continue;
        }
        f.set(entity, DataTypeUtil.convertData(values.get(f.getName()), f.getType()));
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
    return entity;
  }

  public static <T> T createObject(
      Class<T> clazz, Object[] items, int start, int end, String... fieldNames) {
    try {
      T entity = (T) clazz.newInstance();
      fill(entity, items, start, end, fieldNames);
      return entity;
    } catch (Exception ex) {
      return null;
    }
  }

  public static void fill(Object entity, Object[] items, int start, int end, String... fieldNames) {
    Map<String, Field> fields = getFields(entity.getClass());
    for (int i = 0, j = start; i < fieldNames.length && j <= end; ++i, j++) {
      try {
        if (!fields.containsKey(fieldNames[i])) {
          continue;
        }
        Field f = fields.get(fieldNames[i]);
        ReflectionUtils.makeAccessible(f);
        f.set(entity, DataTypeUtil.convertData(items[j], f.getType()));
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  public static void fillNull(Object entity, String... ignoreFields) {
    List<String> excludeFields = Arrays.asList(ignoreFields);
    for (Field f : entity.getClass().getDeclaredFields()) {
      try {
        ReflectionUtils.makeAccessible(f);
        if (excludeFields.contains(f.getName())) {
          continue;
        }
        f.set(entity, null);
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  public static void fill(Object entity, List<String> fields, Object value) {
    for (Field f : entity.getClass().getDeclaredFields()) {
      try {
        if (!fields.contains(f.getName())) {
          continue;
        }
        ReflectionUtils.makeAccessible(f);
        f.set(entity, value);
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  public static List<String> getLazyEntityFields(Class entityClazz) {
    return getFields(entityClazz).values().stream()
        .filter(
            f -> {
              if (f.getAnnotation(ManyToOne.class) != null
                  && f.getAnnotation(ManyToOne.class).fetch() == FetchType.LAZY) {
                return true;
              }
              if (f.getAnnotation(OneToMany.class) != null
                  && f.getAnnotation(OneToMany.class).fetch() == FetchType.LAZY) {
                return true;
              }
              if (f.getAnnotation(ManyToMany.class) != null
                  && f.getAnnotation(ManyToMany.class).fetch() == FetchType.LAZY) {
                return true;
              }
              return false;
            })
        .map(Field::getName)
        .collect(Collectors.toList());
  }

  public static Object fillNullForLazyFields(Object entity) {
    return fillNullForUnsetLazyFields(entity);
  }

  public static Object fillNullForUnsetLazyFields(Object entity) {
    for (String field : EntityUtils.getLazyEntityFields(entity.getClass())) {
      try {
        Object o = EntityUtils.getFieldValue(entity, field);
        if (isObjectProxy(o)) {
          EntityUtils.setFieldValue(entity, field, null);
        } else if (isEntity(o)) {
          fillNullForUnsetLazyFields(o);
        }
      } catch (Exception ex) {
        EntityUtils.setFieldValue(entity, field, null);
      }
    }
    return entity;
  }

  public static boolean isObjectProxy(Object o) {
    if (o == null) {
      return false;
    }
    if (o.getClass().getName().startsWith("org.hibernate.collection.")) {
      try {
        int size = ((Collection)o).size();
        if (size > 0) {
          (((Collection)o).iterator()).hasNext();
        }
        return false;
      } catch (Exception ex) {
        return true;
      }
    }
    return o instanceof HibernateProxy || o.getClass().getName().contains("$HibernateProxy");
  }

  public static <T> T unProxyEntity(T entity) {
    if (entity == null) {
      throw new RuntimeException("Entity to unProxyEntity must not be null");
    }

    Hibernate.initialize(entity);
    if (entity instanceof HibernateProxy) {
      entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
    }
    return entity;
  }

  public static String getIdFieldName(Class entityClass) {
    if (entityClass == null) {
      return null;
    }
    return Stream.of(entityClass.getDeclaredFields())
        .filter(f -> f.getAnnotation(Id.class) != null || f.getAnnotation(EmbeddedId.class) != null)
        .map(Field::getName)
        .findFirst()
        .orElse(null);
  }

  public static Class getFieldType(Class clazz, String fieldName) {
    return getField(clazz, fieldName).getType();
  }

  public static Field getField(Class clazz, String fieldName) {
    if (!fieldName.contains(".")) {
      return getFields(clazz).get(fieldName);
    }
    String[] parts = fieldName.split("\\s*\\.\\s*");
    Field result = null;
    for (int i = 0; i < parts.length; ++i) {
      result = getField(clazz, parts[i]);
      clazz = result.getType();
      if (i < parts.length - 1
          && (Map.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz))) {
        clazz = getEntityArgumentType(result);
      }
    }
    return result;
  }

  public static boolean isEntityClass(Class<?> clazz) {
    return clazz.getAnnotation(Entity.class) != null;
  }

  public static boolean isEntity(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof HibernateProxy) {
      return true;
    }
    return isEntityClass(obj.getClass());
  }

  public static Object getId(Object entity) {
    if (entity == null) {
      return null;
    }
    try {
      if (entity instanceof UUID) {
        return (UUID) entity;
      }
      if (entity instanceof String) {
        return UUIDUtils.fromStringSafe(entity.toString());
      }
      Field idField =
          Stream.of(entity.getClass().getDeclaredFields())
              .filter(
                  f ->
                      f.getAnnotation(Id.class) != null
                          || f.getAnnotation(EmbeddedId.class) != null)
              .findFirst()
              .orElse(null);
      if (idField == null) {
        return null;
      }
      return ReflectionUtils.invokeMethod(
          entity
              .getClass()
              .getMethod(String.format("get%s", StringUtils.capitalize(idField.getName()))),
          entity);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
    return null;
  }

  public static void setId(Object entity, Object newId) {
    if (entity == null) {
      return;
    }
    try {
      Field idField =
          Stream.of(entity.getClass().getDeclaredFields())
              .filter(f -> f.getAnnotation(Id.class) != null)
              .findFirst()
              .orElse(null);
      if (idField == null) {
        return;
      }
      ReflectionUtils.invokeMethod(
          entity
              .getClass()
              .getMethod(
                  String.format("set%s", StringUtils.capitalize(idField.getName())),
                  newId.getClass()),
          entity,
          newId);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  public static Object merge(Object source, Object target, String... ignoreProps) {
    Set<String> ignoreProperties = new HashSet<>(Arrays.asList(ignoreProps));
    ignoreProperties.addAll(getNullPropertyNames(source));
    if (EntityUtils.isEntity(source) || EntityUtils.isEntity(target)) {
      // Default doesn't merge OneToMany field in the source entity
      ignoreProperties.addAll(getCollectionEntityFields(source));
      ignoreProperties.addAll(getCollectionEntityFields(target));
    }
    BeanUtils.copyProperties(
        source, target, ignoreProperties.toArray(new String[ignoreProperties.size()]));
    PropertyDescriptor[] props = BeanUtils.getPropertyDescriptors(target.getClass());
    for (PropertyDescriptor prop : props) {
      Method propReadMethod = prop.getReadMethod();
      if (propReadMethod == null) {
        continue;
      }
      try {
        Object sourceValue = source.getClass().getMethod(propReadMethod.getName()).invoke(source);
        Object targetValue = propReadMethod.invoke(target);
        if (sourceValue != null && targetValue == null) {
          EntityUtils.setFieldValue(
              target,
              prop.getName(),
              DataTypeUtil.convertData(sourceValue, prop.getPropertyType()));
        }
      } catch (Exception ex) {
        continue;
      }
    }
    return target;
  }

  public static void setFieldValue(Object obj, String fieldName, Object value) {
    setFieldValue(obj, getField(obj.getClass(), fieldName), value);
  }

  public static Object invokeMethod(Object obj, String methodName, Object... params) {
    try {
      Method m =
          obj.getClass()
              .getMethod(
                  methodName,
                  Arrays.stream(params).map(Object::getClass).toArray(size -> new Class[size]));
      m.setAccessible(true);
      return m.invoke(obj, params);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      return null;
    }
  }

  public static void setFieldValue(Object obj, Field field, Object value) {
    if (field == null) {
      return;
    }
    try {
      ReflectionUtils.makeAccessible(field);
      ReflectionUtils.setField(field, obj, value);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  private static Set<String> getCollectionEntityFields(Object entity) {
    if (entity == null || entity.getClass().getAnnotation(Entity.class) == null) {
      return Collections.emptySet();
    }
    return Stream.of(entity.getClass().getDeclaredFields())
        .filter(
            f ->
                f.getAnnotation(OneToMany.class) != null
                    || f.getAnnotation(ManyToMany.class) != null)
        .map(f -> f.getName())
        .collect(Collectors.toSet());
  }

  private static Set<String> getNullPropertyNames(Object source) {
    final BeanWrapper src = new BeanWrapperImpl(source);
    PropertyDescriptor[] pds = src.getPropertyDescriptors();

    Set<String> emptyNames = new HashSet<>();
    for (PropertyDescriptor pd : pds) {
      Object srcValue = src.getPropertyValue(pd.getName());
      if (srcValue == null) {
        emptyNames.add(pd.getName());
      }
    }
    return emptyNames;
  }

  private static Map<String, Field> getFields(Class clazz) {
    if (clazzFields.containsKey(clazz)) {
      return clazzFields.get(clazz);
    }
    Map fields = new HashMap<>();
    Class parent = clazz;
    do {
      Stream.of(parent.getDeclaredFields())
          .filter(f -> !fields.containsKey(f.getName()))
          .forEach(f -> fields.put(f.getName(), f));
      parent = parent.getSuperclass();
    } while (parent != null
        && (parent.getAnnotation(Entity.class) != null
            || parent.getAnnotation(MappedSuperclass.class) != null));

    clazzFields.put(clazz, fields);
    return fields;
  }

  public static List<String> getDeclaredFields(Class clazz) {
    return Arrays.asList(clazz.getDeclaredFields()).stream()
        .map(f -> f.getName())
        .collect(Collectors.toList());
  }

  public static Map<String, Field> getFields(Class clazz, Class fieldType) {
    return getFields(clazz).entrySet().stream()
        .filter(e -> e.getValue().getType().isAssignableFrom(fieldType))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static Object getFieldValue(Object o, String field) {
    try {
      if (o == null) {
        return null;
      }

      Field f = getField(o.getClass(), field);
      return getFieldValue(o, f);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
    return null;
  }

  private static Object getFieldValue(Object o, Field f) {
    try {
      if (o == null || f == null) {
        return null;
      }
      ReflectionUtils.makeAccessible(f);
      return f.get(o);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
    return null;
  }

  public static Object getProperty(Object o, String propName) {
    try {
      if (o == null) {
        return null;
      }
      return ReflectionUtils.invokeMethod(
          o.getClass().getMethod(String.format("get%s", StringUtils.capitalize(propName))), o);
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
    return null;
  }

  public static Class getEntityArgumentType(Field field) {
    Class containerType = field.getType();

    if (containerType.isAssignableFrom(Set.class)) {
      return (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }
    if (containerType.isAssignableFrom(List.class)) {
      return (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }
    if (containerType.isAssignableFrom(Map.class)) {
      return (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
    }
    return null;
  }

  public static List<String> getEmptiedValueFields(Object entity) {
    if (entity == null) {
      return Collections.emptyList();
    }
    List<String> fieldNames = new ArrayList<>();
    for (Field f : getFields(entity.getClass()).values()) {
      try {
        Object value = getFieldValue(entity, f.getName());
        if (value == null) {
          fieldNames.add(f.getName());
        } else {
          if ("".equals(value.toString())) {
            fieldNames.add(f.getName());
          } else if (value instanceof Timestamp && ((Timestamp) value).getTime() == 0) {
            fieldNames.add(f.getName());
          } else if (value instanceof Collection && ((Collection) value).isEmpty()) {
            fieldNames.add(f.getName());
          }
        }
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
    return fieldNames;
  }
}
