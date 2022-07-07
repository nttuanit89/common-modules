package com.learning.ftp.common.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import static java.util.Optional.ofNullable;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BaseLoggerFactory {
  private static final String APPLICATION_NAME = "spring.application.name";
  private static ApplicationContext applicationContext;

  public static Logger getLogger(Class<?> cls) {
    Logger logger = LoggerFactory.getLogger(cls);
    boolean hasServiceName = hasPrefixInLogMessagePattern(logger, "[%X{serviceName}]");
    return (Logger)
        Proxy.newProxyInstance(
            logger.getClass().getClassLoader(),
            new Class<?>[] {Logger.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "info":
                case "warn":
                case "debug":
                case "error":
                  addMorePrefix(
                      hasServiceName, "serviceName", BaseLoggerFactory::getServiceName);
                  // Add more service startup info here
              }
              return method.invoke(logger, args);
            });
  }

  public static void setApplicationContext(ApplicationContext applicationContext) {
    BaseLoggerFactory.applicationContext = applicationContext;
  }

  private static void addMorePrefix(boolean hasPrefix, String key, Supplier<String> valueSupplier) {
    if (hasPrefix) {
      MDC.put(key, valueSupplier.get());
    }
  }

  private static boolean hasPrefixInLogMessagePattern(Logger logger, String prefix) {
    ch.qos.logback.classic.Logger springLogger = (ch.qos.logback.classic.Logger) logger;
    while (springLogger != null && !springLogger.iteratorForAppenders().hasNext()) {
      springLogger = getParentLogger(springLogger);
    }
    if (springLogger == null) {
      return false;
    }
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                springLogger.iteratorForAppenders(), Spliterator.ORDERED),
            false)
        .filter(ConsoleAppender.class::isInstance)
        .anyMatch(
            appender -> {
              try {

                Method getEncoder = appender.getClass().getMethod("getEncoder");
                return ((PatternLayoutEncoder) getEncoder.invoke(appender))
                    .getPattern()
                    .contains(prefix);
              } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                return false;
              }
            });
  }

  private static String getServiceName() {
    return ofNullable(applicationContext)
        .map(ApplicationContext::getEnvironment)
        .map(env -> env.getProperty(APPLICATION_NAME))
        .orElseGet(() -> System.getProperty(APPLICATION_NAME, EMPTY));
  }

  private static ch.qos.logback.classic.Logger getParentLogger(
      ch.qos.logback.classic.Logger logger) {
    try {
      Field parentField = logger.getClass().getDeclaredField("parent");
      parentField.setAccessible(true);
      return (ch.qos.logback.classic.Logger) parentField.get(logger);
    } catch (Exception ex) {
      System.out.println("Exception getParentLogger: " + ex.getMessage());
      return null;
    }
  }
}
