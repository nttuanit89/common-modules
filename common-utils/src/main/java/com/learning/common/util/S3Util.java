package com.learning.common.util;

import com.google.common.collect.ImmutableMap;
import com.learning.proto.common.SignedType;
import static com.learning.proto.common.SignedType.BACK_ID;
import static com.learning.proto.common.SignedType.BOND_DESCRIPTION;
import static com.learning.proto.common.SignedType.BUSINESS_LICENSE;
import static com.learning.proto.common.SignedType.CONTRACT;
import static com.learning.proto.common.SignedType.FRONT_ID;
import static com.learning.proto.common.SignedType.ISSUER_LOGO;
import static com.learning.proto.common.SignedType.MANUAL_CONTRACT;
import static com.learning.proto.common.SignedType.OFFERING_CIRCULAR;
import static com.learning.proto.common.SignedType.PERSONAL_PORTRAIT;
import static com.learning.proto.common.SignedType.PROFESSIONAL_INVESTOR;
import static com.learning.proto.common.SignedType.TEMPLATE;
import static com.learning.proto.common.SignedType.TERM_SHEET;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import static java.time.Duration.ofMillis;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.function.Function.identity;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.joinWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@SuppressWarnings({"UnusedReturnValue", "unused"})
@Component
@Lazy
public class S3Util {
  private final ExecutorService pool = Executors.newWorkStealingPool(9);
  private final S3Presigner presigner;
  private final S3Client s3Client;

  @Value("${finbase.s3-config.upload-timeout:10}")
  private long uploadTimeoutInMinutes;

  @Value("${finbase.s3-config.download-timeout:5}")
  private long downloadTimeoutInMinutes;

  @Value("${finbase.s3-config.bucket-template:}")
  private String bucketTemplate;

  @Value("${finbase.s3-config.bucket-image:}")
  private String bucketImage;

  @Value("${finbase.s3-config.bucket-contract:}")
  private String bucketContract;

  private final Map<SignedType, String> defaultExtensions =
      ImmutableMap.<SignedType, String>builder()
          .put(CONTRACT, ".pdf")
          .put(FRONT_ID, ".jpeg")
          .put(BACK_ID, ".jpeg")
          .put(PERSONAL_PORTRAIT, ".jpeg")
          .build();

  private final Map<SignedType, String> folderS3 =
      ImmutableMap.<SignedType, String>builder()
          .put(CONTRACT, "contract")
          .put(MANUAL_CONTRACT, "contract/manual")
          .put(OFFERING_CIRCULAR, "bond/offering-circular")
          .put(TERM_SHEET, "bond/term-sheet")
          .put(FRONT_ID, "user-profile")
          .put(BACK_ID, "user-profile")
          .put(PERSONAL_PORTRAIT, "user-profile")
          .put(BUSINESS_LICENSE, "user-profile")
          .put(ISSUER_LOGO, "bond/issuer")
          .put(BOND_DESCRIPTION, "bond/description")
          .put(PROFESSIONAL_INVESTOR, "user-profile/professional-investor")
          .build();

  private final Map<SignedType, Supplier<String>> bucketS3 =
      ImmutableMap.<SignedType, Supplier<String>>builder()
          .put(CONTRACT, () -> bucketContract)
          .put(TEMPLATE, () -> bucketTemplate)
          .build();

  S3Util(
      @Value("${finbase.s3-config.region:ap-southeast-1}") String regionName,
      @Value("${finbase.s3-config.endpoint:}") String endpoint) {
    this.presigner = getS3Presigner(endpoint, regionName);
    this.s3Client = getS3Client(endpoint, regionName);
  }

  private S3Presigner getS3Presigner(String endpoint, String regionName) {
    S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(regionName));
    ofNullable(endpoint)
        .filter(StringUtils::isNotBlank)
        .map(URI::create)
        .ifPresent(builder::endpointOverride);
    return builder.build();
  }

  private S3Client getS3Client(String endpoint, String regionName) {
    S3ClientBuilder builder = S3Client.builder().region(Region.of(regionName));
    ofNullable(endpoint)
        .filter(StringUtils::isNotBlank)
        .map(URI::create)
        .ifPresent(builder::endpointOverride);
    return builder.build();
  }

  public List<UploadResponse> getUploadURLs(UploadRequest request) {
    return parallel(
        () ->
            request.getTypes().parallelStream()
                .map(type -> getUploadURL(request, type))
                .collect(toList()));
  }

  private <T> T parallel(Callable<T> supplier) {
    try {
      return pool.submit(supplier).get(1L, MINUTES);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public UploadResponse getUploadURL(UploadRequest dto, SignedType type) {
    String key =
        joinWith(
            "/",
            dto.getCompany(),
            folderS3.getOrDefault(type, "other"),
            dto.getUserId(),
            generateFileName(dto, type));
    long timeout = uploadTimeoutInMinutes * 60L * 1000;
    String bucketName = bucketS3.getOrDefault(type, () -> bucketImage).get();
    String path = generatePreSignedUploadURL(timeout, key, bucketName);
    return new UploadResponse(type, key, path);
  }

  private String generateFileName(UploadRequest dto, SignedType type) {
    return join(
        randomUUID().toString(),
        defaultIfBlank(
            joinFileNameDto(dto.getFileName()), defaultExtensions.getOrDefault(type, EMPTY)));
  }

  private String joinFileNameDto(String fileName) {
    if (isBlank(fileName)) {
      return EMPTY;
    }

    return join("_", fileName);
  }

  private String generatePreSignedUploadURL(long milliseconds, String path, String bucketName) {
    PutObjectRequest objReq = PutObjectRequest.builder().bucket(bucketName).key(path).build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(ofMillis(milliseconds))
            .putObjectRequest(objReq)
            .build();

    return presigner.presignPutObject(presignRequest).url().toString();
  }

  private String generatePreSignedDownloadURL(
      long milliseconds, String fileKey, String bucketName) {
    if (isAnyBlank(fileKey, bucketName)) {
      return EMPTY;
    }

    GetObjectRequest objReq = GetObjectRequest.builder().bucket(bucketName).key(fileKey).build();
    GetObjectPresignRequest presignReq =
        GetObjectPresignRequest.builder()
            .signatureDuration(ofMillis(milliseconds))
            .getObjectRequest(objReq)
            .build();

    return presigner.presignGetObject(presignReq).url().toString();
  }

  public Map<DownloadRequest, String> getDownloadURLs(List<DownloadRequest> dtos) {
    return parallel(
        () -> dtos.stream().distinct().parallel().collect(toMap(identity(), this::getDownloadURL)));
  }

  public String getDownloadURL(DownloadRequest request) {
    return generatePreSignedDownloadURL(
        downloadTimeoutInMinutes * 60L * 1000,
        request.getPath(),
        bucketS3.getOrDefault(request.getType(), () -> bucketImage).get());
  }

  public InputStream downloadObject(String uri, SignedType type) {
    return downloadObject(uri, bucketS3.getOrDefault(type, () -> bucketImage).get());
  }

  public InputStream downloadObject(String uri, String bucket) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(uri).build();
    return s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asInputStream();
  }

  public void uploadObject(File file, String uri, SignedType type) {
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(bucketS3.getOrDefault(type, () -> bucketImage).get())
            .key(uri)
            .build();
    s3Client.putObject(objectRequest, RequestBody.fromFile(file));
  }

  public void uploadObject(String content, String uri, SignedType type) {
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(bucketS3.getOrDefault(type, () -> bucketImage).get())
            .key(uri)
            .build();
    s3Client.putObject(objectRequest, RequestBody.fromString(content));
  }

  public void deleteObject(String uri, SignedType type) {
    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder()
            .bucket(bucketS3.getOrDefault(type, () -> bucketImage).get())
            .key(uri)
            .build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  @lombok.Value
  public static class DownloadRequest {
    String path;
    SignedType type;
  }

  @lombok.Value
  public static class UploadRequest {
    List<SignedType> types;
    String company;
    String userId;
    String fileName;
  }

  @lombok.Value
  public static class UploadResponse {
    SignedType type;
    String key;
    String path;
  }
}
