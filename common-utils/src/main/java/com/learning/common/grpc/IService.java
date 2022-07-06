package com.learning.common.grpc;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.protobuf.Message;
import com.learning.common.exception.FtpServiceException;
import com.learning.common.exception.ServiceException;
import com.learning.common.logging.BaseLoggerFactory;
import com.learning.proto.common.Status;
import io.grpc.stub.StreamObserver;
import static java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang3.ObjectUtils;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public interface IService {
  Logger LOG = BaseLoggerFactory.getLogger(IService.class);
  BiMap<HttpStatus, io.grpc.Status> GRPC_BUSINESS_STATUSES =
      ImmutableBiMap.<HttpStatus, io.grpc.Status>builder()
          .put(NOT_FOUND, io.grpc.Status.NOT_FOUND)
          .put(UNAUTHORIZED, io.grpc.Status.UNAUTHENTICATED)
          .put(FORBIDDEN, io.grpc.Status.PERMISSION_DENIED)
          .put(BAD_REQUEST, io.grpc.Status.INVALID_ARGUMENT)
          .put(PRECONDITION_FAILED, io.grpc.Status.FAILED_PRECONDITION)
          .put(NOT_IMPLEMENTED, io.grpc.Status.UNIMPLEMENTED)
          .put(INTERNAL_SERVER_ERROR, io.grpc.Status.INTERNAL)
          .build();

  /**
   * Build the message response.
   *
   * @param <V> the given type of response message
   */
  default <V extends Message> V makeResponse(
      ResponseCreator<V> rc, Class<V> responseType, boolean... throwEx) {
    V.Builder builder = null;
    boolean isThrowEx = throwEx.length > 0 && throwEx[0];
    logCaller();
    try {
      builder = (V.Builder) responseType.getMethod("newBuilder").invoke(null);
      builder = rc.createResponse().toBuilder();
      Method updateStatus = builder.getClass().getMethod("mergeStatus", Status.class);
      updateStatus.invoke(builder, Status.getDefaultInstance());
    } catch (FtpServiceException e) {
      logException(e);
      if (isThrowEx) {
        throw e;
      }
      handleError(builder, Status.newBuilder().setMessageCode(e.getMessageCode()), e);
    } catch (Throwable e) {
      logException(e);
      if (isThrowEx) {
        throw new FtpServiceException("0000000", e.getMessage(), INTERNAL_SERVER_ERROR);
      }
      handleError(builder, Status.newBuilder(), e);
    }

    return (V) builder.build();
  }

  private void logCaller() {
    Predicate<String> baseCaller = cn -> cn.startsWith("com.learning.");
    Predicate<String> controller = cn -> cn.contains("Controller");
    Predicate<String> service =
        cn -> cn.contains("Service") && !cn.contains(IService.class.getName());
    Predicate<StackFrame> caller =
        st -> baseCaller.and(service.or(controller)).test(st.getClassName());
    Function<StackFrame, String> formatter =
        st ->
            "Execute "
                + st.getClassName().substring(st.getClassName().lastIndexOf(".") + 1)
                + "."
                + st.getMethodName();

    StackWalker.getInstance()
        .walk(s -> s.filter(caller).limit(1).map(formatter).collect(toList()))
        .forEach(LOG::info);
  }

  private void handleError(Message.Builder builder, Status.Builder status, Throwable e) {
    status.setMessage(e.getMessage());
    status.setCode(getCodeFrom(e));
    try {
      Method updateStatus = builder.getClass().getMethod("mergeStatus", Status.class);
      updateStatus.invoke(builder, status.build());
    } catch (Exception ex) {
      LOG.error("service error: " + e.getMessage(), e);
    }
  }

  /**
   * Build the message response.
   *
   * @param responseObserver the given {@link StreamObserver}
   * @param <T> the given type of request message
   * @param <V> the given type of response message
   */
  default <T extends Message, V extends Message> void response(
      ResponseCreator<V> rc, Class<V> responseType, StreamObserver<V> responseObserver) {
    buildResponse(makeResponse(rc, responseType), responseObserver);
  }

  /**
   * Build the message response.
   *
   * @param resp the given response
   * @param streamObserver the given the given {@link StreamObserver}
   * @param <V> the given type of response message
   */
  default <V extends Message> void buildResponse(V resp, StreamObserver streamObserver) {
    // In any case we always return the response object, in case error the status in the response
    // contains message error code
    if (streamObserver != null) {
      streamObserver.onNext(resp);
      streamObserver.onCompleted();
    }
  }

  /**
   * Catch exception.
   *
   * @param e the given {@link Exception}
   * @return an instance of {@link io.grpc.Status}
   */
  private int getCodeFrom(Throwable e) {
    io.grpc.Status status;
    if (e instanceof ServiceException) {
      status = ((ServiceException) e).getStatus();
    } else {
      io.grpc.Status grpcStatus =
          Optional.of(e)
              .filter(FtpServiceException.class::isInstance)
              .map(FtpServiceException.class::cast)
              .map(FtpServiceException::getHttpStatus)
              .map(GRPC_BUSINESS_STATUSES::get)
              .orElse(io.grpc.Status.UNKNOWN);

      status = grpcStatus.withCause(e).withDescription(e.getMessage());
    }

    return status.getCode().value();
  }

  /**
   * Log exception.
   *
   * @param e the given {@link Exception}
   */
  private void logException(Throwable e) {
    Predicate<String> baseCaller = cn -> cn.startsWith("com.learning.");
    Predicate<String> controller = cn -> cn.contains("Controller");
    Predicate<StackFrame> fromController =
        st -> baseCaller.and(controller).test(st.getClassName());
    if (StackWalker.getInstance().walk(s -> s.anyMatch(fromController))) {
      // Let ControllerAdvice do the job
      return;
    }

    String message = e.getMessage();
    if (isEmpty(message) && e.getCause() != null) {
      message = e.getCause().getMessage();
    }

    Throwable stackTrace = ObjectUtils.defaultIfNull(e.getCause(), e);
    if (isBusinessException(e)) {
      LOG.warn(message);
    } else {
      LOG.error(message, stackTrace);
    }
  }

  private boolean isBusinessException(Throwable e) {
    return Optional.ofNullable(e)
        .filter(FtpServiceException.class::isInstance)
        .map(FtpServiceException.class::cast)
        .map(FtpServiceException::getHttpStatus)
        .filter(HttpStatus::is4xxClientError)
        .isPresent();
  }

  interface ResponseCreator<V> {
    V createResponse();
  }
}
