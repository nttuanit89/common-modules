package com.learning.common.util;

import com.google.common.hash.Hashing;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.StringUtils;

import javax.crypto.KeyGenerator;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SignRequestUtil {
  public static String[] genAccessSecretKeyPair() throws Exception {
    KeyGenerator generator = KeyGenerator.getInstance("HMACSHA256");
    generator.init(120);
    byte[] accessKeyId = generator.generateKey().getEncoded();
    generator.init(240);
    byte[] secretAccessKey = generator.generateKey().getEncoded();
    return Arrays.asList(
            Base64.getEncoder().encodeToString(accessKeyId),
            Base64.getEncoder().encodeToString(secretAccessKey))
        .toArray(new String[] {});
  }

  public static String createRequestSignature(
      String secretKey, String service, byte[] body, HttpServletRequest request) throws Exception {
    return sign(Base64.getDecoder().decode(secretKey), service, body, request);
  }

  public static boolean isValidRequest(
      String secretKey, String service, byte[] body, HttpServletRequest request) throws Exception {
    return sign(Base64.getDecoder().decode(secretKey), service, body, request)
        .equals(extractSignature(request));
  }

  public static String sign(String secretKey, String date, String service, String... lines) {
    String requestItemLines = String.join("\n", Arrays.asList(lines));
    return signRequest(Base64.getDecoder().decode(secretKey), date, service, requestItemLines);
  }

  private static String sign(
      byte[] secretKey, String service, byte[] body, HttpServletRequest request) throws Exception {
    String requestItemLines = extractRequestItemLines(body, request);
    return signRequest(secretKey, request.getHeader("X-Fin-Date"), service, requestItemLines);
  }

  private static String signRequest(
      byte[] secretKey, String date, String service, String requestItemLines) {
    String signString = createSignString("HMAC-SHA256", date, requestItemLines);
    byte[] signKey = createSignKey(secretKey, date, service);
    return toHex(
        Hashing.hmacSha256(signKey)
            .hashBytes(signString.getBytes(StandardCharsets.UTF_8))
            .asBytes());
  }

  private static byte[] createSignKey(byte[] secretKey, String date, String service) {
    return toHMACSHA256Hash(
        toHMACSHA256Hash(
            toHMACSHA256Hash(
                combine("FIN1".getBytes(StandardCharsets.UTF_8), secretKey),
                date.replaceAll("T.+", "")),
            service),
        "fin1_int_request");
  }

  private static String createSignString(
      String algorithm, String requestDateTime, String requestContent) {
    return new StringBuilder()
        .append(algorithm)
        .append("\n")
        .append(requestDateTime)
        .append("\n")
        .append(toHexSHA256Hash(requestContent))
        .toString();
  }

  private static String extractRequestItemLines(byte[] body, HttpServletRequest request)
      throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append(request.getMethod())
        .append("\n")
        .append(request.getRequestURI())
        .append("\n")
        .append(request.getQueryString())
        .append("\n");
    List<String> signHeaders = extractSignHeaders(request);
    for (String header : signHeaders) {
      sb.append(header)
          .append(": \"")
          .append(
              URLEncoder.encode(
                  extractHeaderValue(request, header), StandardCharsets.UTF_8.toString()))
          .append("\"\n");
    }
    sb.append(String.join(";", signHeaders)).append("\n").append(toHexSHA256Hash(body));
    return sb.toString().replaceAll("\nnull", "\n").trim();
  }

  public static String extractSignature(HttpServletRequest request) {
    return extractSignInfo(request, "signature");
  }

  private static List<String> extractSignHeaders(HttpServletRequest request) {
    return Arrays.asList(extractSignInfo(request, "signedheaders").split(";"));
  }

  private static String extractSignInfo(HttpServletRequest request, String key) {
    return Arrays.stream(request.getHeader("Authorization").toLowerCase().split("\\s*,\\s*"))
        .collect(Collectors.toMap(p -> p.split("\\s*=\\s*")[0], p -> p.split("\\s*=\\s*")[1]))
        .get(key);
  }

  private static String toHex(byte[] bytes) {
    return new String(Hex.encode(bytes));
  }

  private static String toHexSHA256Hash(String content) {
    return toHex(toSHA256Hash(content));
  }

  private static String extractHeaderValue(HttpServletRequest request, String header) {
    String value = request.getHeader(header);
    if (value != null && header.equals("host")) {
      return value.replaceAll("(http|https)://", "");
    }
    return StringUtils.isEmpty(value) ? "" : value;
  }

  public static String toHexSHA256Hash(Object content) {
    if (content instanceof String) {
      return toHex(toSHA256Hash((String) content));
    }
    if (content instanceof byte[]) {
      return toHex(toSHA256Hash((byte[]) content));
    }
    return toHex(
        toSHA256Hash(JsonMapper.toString(JsonMapper.getMapper().convertValue(content, Map.class))));
  }

  private static String toHexSHA256Hash(byte[] content) {
    return toHex(toSHA256Hash(content));
  }

  private static byte[] toSHA256Hash(String content) {
    return Hashing.sha256().hashString(content, StandardCharsets.UTF_8).asBytes();
  }

  private static byte[] toSHA256Hash(byte[] content) {
    return Hashing.sha256().hashBytes(content).asBytes();
  }

  private static byte[] toHMACSHA256Hash(String key, String content) {
    return toHMACSHA256Hash(key.getBytes(StandardCharsets.UTF_8), content);
  }

  private static byte[] toHMACSHA256Hash(byte[] key, String content) {
    return Hashing.hmacSha256(key).hashBytes(content.getBytes(StandardCharsets.UTF_8)).asBytes();
  }

  private static final byte[] combine(byte[] first, byte[] second) {
    byte[] dest = new byte[first.length + second.length];

    System.arraycopy(first, 0, dest, 0, first.length);
    System.arraycopy(second, 0, dest, first.length, second.length);

    return dest;
  }
}
