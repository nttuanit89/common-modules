package com.learning.common.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.learning.common.exception.FtpServiceException;
import com.learning.common.grpc.parser.ProtoEntityMapper;
import com.learning.common.logging.BaseLoggerFactory;
import com.learning.proto.common.Status;
import io.grpc.stub.AbstractStub;
import java.util.Arrays;
import static lombok.AccessLevel.PROTECTED;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

@SuppressWarnings("unused")
@RequiredArgsConstructor(access = PROTECTED)
public abstract class AbstractServiceInvoking {
  private final Logger log = BaseLoggerFactory.getLogger(getClass());
  private final GRPCStubProvider stubProvider;
  private final String serviceName;

  protected <S extends AbstractStub<S>> S getStub(Class<S> sClass) {
    return stubProvider.getBlockingStub(serviceName, sClass);
  }

  protected <T extends Message> void log(String method, T request) {
    if (log.isDebugEnabled()) {
      log.debug("Grpc call {}: {}", method, format(request));
    } else {
      log.info("Grpc call {}", method);
    }
  }

  private <T extends Message> String format(T request) {
    return ProtoEntityMapper.toString(request);
  }

  private boolean verifyStatus(io.grpc.Status grpcStatus, io.grpc.Status... validStatus) {
    return Arrays.stream(validStatus)
        .map(io.grpc.Status::getCode)
        .anyMatch(grpcStatus.getCode()::equals);
  }

  protected <T extends Message> T verifyResult(T result) {
    if (log.isDebugEnabled()) {
      log.debug("Grpc response: {}", format(result));
    }
    verifyResponseStatus(getStatus(result));
    return result;
  }

  protected void verifyResponseStatus(Status status) {
    io.grpc.Status grpcStatus = io.grpc.Status.fromCodeValue(status.getCode());
    if (!verifyStatus(grpcStatus, io.grpc.Status.OK)) {
      HttpStatus httpStatus = fromStatus(grpcStatus);
      throw new FtpServiceException(status.getMessageCode(), status.getMessage(), httpStatus);
    }
  }

  protected <T extends Message> boolean verifyOptionalResult(T result) {
    if (log.isDebugEnabled()) {
      log.debug("Grpc response: {}", format(result));
    }

    Status status = getStatus(result);
    io.grpc.Status grpcStatus = io.grpc.Status.fromCodeValue(status.getCode());
    if (!verifyStatus(grpcStatus, io.grpc.Status.OK, io.grpc.Status.NOT_FOUND)) {
      HttpStatus httpStatus = fromStatus(grpcStatus);
      throw new FtpServiceException(status.getMessageCode(), status.getMessage(), httpStatus);
    }

    return verifyStatus(grpcStatus, io.grpc.Status.OK);
  }

  private Status getStatus(Message result) {
    Descriptors.Descriptor description = result.getDescriptorForType();
    Descriptors.FieldDescriptor statusField = description.findFieldByName("status");
    return (Status) result.getField(statusField);
  }

  private HttpStatus fromStatus(io.grpc.Status status) {
    return IService.GRPC_BUSINESS_STATUSES.inverse().getOrDefault(status, HttpStatus.BAD_REQUEST);
  }
}
