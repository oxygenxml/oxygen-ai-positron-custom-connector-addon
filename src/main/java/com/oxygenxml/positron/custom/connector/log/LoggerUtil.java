/*
 *   Copyright 2025 Syncro Soft SRL
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * `*   `[`http://www.apache.org/licenses/LICENSE-2.0`](http://www.apache.org/licenses/LICENSE-2.0)
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.oxygenxml.positron.custom.connector.log;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class for logging
 *
 * @author cosmin_duna
 *
 */
public class LoggerUtil {

  /**
   * Number of characters of the actual key shown in masked header values.
   * Chosen to expose enough to identify which key is in use without revealing it.
   */
  private static final int HEADER_VALUE_VISIBLE_CHARS = 6;

  /** Marker used when message text content (user/assistant turns) is filtered. */
  private static final String MARKER_TEXT_CONTENT   = "[filtered: text content]";

  /** Marker used when a system prompt is filtered. */
  private static final String MARKER_SYSTEM_PROMPT  = "[filtered: system prompt]";

  /** Marker used when an input field is filtered. */
  private static final String MARKER_INPUT          = "[filtered: input]";

  /** Marker used when a function/tool description is filtered. */
  private static final String MARKER_DESCRIPTION    = "[filtered: description]";

  /** Marker used when image binary or base64 data is filtered. */
  private static final String MARKER_IMAGE_DATA     = "[filtered: image data]";

  /**
   * The prefix of the Authorization bearer token header.
   */
  private static final String BEARER_HEADER_PREFIX = "bearer ";

  /**
   * A 1×1 transparent PNG encoded in base64, used as a placeholder when filtering image
   * {@code data} / {@code bytes} fields. Keeping a structurally valid image means the
   * logged request body can be replayed in external tools without API validation errors
   * (e.g. "invalid base64 data" or "Could not process image").
   */
  private static final String DUMMY_IMAGE_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

  /**
   * A complete data URL wrapping {@link #DUMMY_IMAGE_BASE64}, used as a placeholder when
   * filtering OpenAI-style {@code url} fields that contain a base64 data URL
   * (e.g. {@code "data:image/jpeg;base64,..."}).
   */
  private static final String DUMMY_IMAGE_DATA_URL = "data:image/png;base64," + DUMMY_IMAGE_BASE64;

  /**
   * Fields whose values must remain syntactically valid base64 after filtering.
   * These carry raw binary/image data (not URLs or plain text).
   */
  private static final Set<String> BASE64_FIELDS = Set.of("data", "bytes");

  /**
   * Maps JSON field names that carry sensitive data to their log marker.
   * Arrays and objects under these names are recursed into rather than replaced,
   * so the request structure stays intact.
   */
  private static final Map<String, String> SENSITIVE_FIELDS = Map.of(
      "content",     MARKER_TEXT_CONTENT,
      "text",        MARKER_TEXT_CONTENT,
      "system",      MARKER_SYSTEM_PROMPT,
      "input",       MARKER_INPUT,
      "description", MARKER_DESCRIPTION,
      "data",        MARKER_IMAGE_DATA,
      "bytes",       MARKER_IMAGE_DATA
  );

  /**
   * Constructor.
   *
   * @throws UnsupportedOperationException when invoked.
   */
  private LoggerUtil() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }


  /**
   * Writes the given object as a JSON string with pretty-print formatting.
   *
   * @param obj The object to serialize.
   *
   * @return  A pretty-printed JSON string representing the given object.
   */
  public static String writeValueAsPrettyJsonString(Object obj) {
    try {
      ObjectMapper objectMapper = defaultObjectMapper();
      DefaultPrettyPrinter.Indenter indenter =
          new DefaultIndenter("    ", "\n");
      DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
      printer.indentObjectsWith(indenter);
      printer.indentArraysWith(indenter);
      ObjectWriter writer = objectMapper.writer(printer);

      return writer.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return e.getMessage();
    }
  }

  /**
   * Filters sensitive data from a JSON request body for safe logging.
   * String values of sensitive fields (message content, system prompts, image data, etc.)
   * are replaced with descriptive markers. Arrays and objects are preserved so the
   * body structure remains usable for reproducing the request in external tools.
   * If the input is not valid JSON it is returned unchanged.
   *
   * @param requestBody The raw JSON request body.
   * @return The filtered request body safe for logging.
   */
  public static String filterMessagesFromRequestBody(String requestBody) {
    ObjectMapper objectMapper = defaultObjectMapper();
    try {
      JsonNode root = objectMapper.readTree(requestBody);
      filterSensitiveFields(root);
      
      DefaultPrettyPrinter.Indenter indenter =
          new DefaultIndenter("    ", "\n");
      DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
      printer.indentObjectsWith(indenter);
      printer.indentArraysWith(indenter);
      ObjectWriter writer = objectMapper.writer(printer);
      
      return writer.writeValueAsString(root);
    } catch (Exception e) {
      return requestBody;
    }
  }

  /**
   * Formats a map of HTTP headers for logging, masking sensitive values such as API keys.
   * Each entry is rendered as {@code name=maskedValue}, all enclosed in braces.
   *
   * @param headers A map of header names to their raw values.
   * @return A string representation of the headers with sensitive values masked.
   */
  public static String formatHeaders(Map<String, String> headers) {
    StringBuilder sb = new StringBuilder("{");
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (sb.length() > 1) {
        sb.append(", ");
      }
      sb.append(entry.getKey()).append("=").append(maskHeaderValue(entry.getValue()));
    }
    return sb.append("}").toString();
  }

  /**
   * Masks a header value by showing only the first {@value #HEADER_VALUE_VISIBLE_CHARS}
   * characters of the key followed by a visible filtered marker, so API keys are
   * partially identifiable in logs without being exposed.
   * A {@code "Bearer "} prefix is preserved in the output but not counted toward the
   * visible characters, ensuring the start of the actual key is always shown.
   * Short values that fit within the visible limit are returned unchanged.
   *
   * @param value The header value to mask.
   * @return The masked header value, or {@code null} if the input is {@code null}.
   */
  public static String maskHeaderValue(String value) {
    if (value == null) {
      return null;
    }
    String visiblePrefix = "";
    String actualValue = value;
    if (value.toLowerCase().startsWith(BEARER_HEADER_PREFIX)) {
      visiblePrefix = value.substring(0, BEARER_HEADER_PREFIX.length());
      actualValue = value.substring(BEARER_HEADER_PREFIX.length());
    }

    if (actualValue.length() <= HEADER_VALUE_VISIBLE_CHARS) {
      return value;
    }

    return visiblePrefix + actualValue.substring(0, HEADER_VALUE_VISIBLE_CHARS) + "**[filtered]**";
  }
    

  /**
   * Recursively filters sensitive fields in a JSON tree node.
   * Only string values of known sensitive fields are replaced with descriptive markers.
   * Arrays and objects are always recursed into so the structure is preserved.
   * Message objects with {@code "role":"system"} or {@code "role":"developer"} have
   * their {@code content} labeled as a system prompt rather than generic text content.
   * Any string value starting with {@code "data:"} is treated as a base64 data URL
   * and filtered regardless of field name.
   * Fields that carry raw binary data ({@code data}, {@code bytes}) are replaced with a
   * valid 1×1 PNG placeholder so the logged body remains replayable. When this substitution
   * happens, a sibling {@code media_type} field (Anthropic format) is updated to
   * {@code image/png}. For the Bedrock Converse format, where {@code bytes} lives inside
   * a nested {@code source} object, the sibling {@code format} field of {@code source}
   * is also updated to {@code "png"}.
   *
   * @param node The JSON node to filter in place.
   */
  private static void filterSensitiveFields(JsonNode node) {
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      List<String> fieldsToProcess = new ArrayList<>();
      obj.fieldNames().forEachRemaining(fieldsToProcess::add);

      JsonNode roleNode = obj.get("role");
      boolean isSystemRole = roleNode != null && roleNode.isTextual()
          && ("system".equals(roleNode.textValue()) || "developer".equals(roleNode.textValue()));

      for (String field : fieldsToProcess) {
        JsonNode child = obj.get(field);
        if (child == null) {
          continue;
        }
        // OpenAI/newer models keep the system prompt in a message with role=system/developer
        String marker = (isSystemRole && "content".equals(field))
            ? MARKER_SYSTEM_PROMPT
            : SENSITIVE_FIELDS.get(field);
        if (marker != null && child.isTextual()) {
          if (BASE64_FIELDS.contains(field)) {
            obj.put(field, DUMMY_IMAGE_BASE64);
            if (obj.has("media_type")) {
              obj.put("media_type", "image/png");
            }
          } else {
            obj.put(field, marker);
          }
        } else if (child.isTextual() && child.textValue().startsWith("data:")) {
          obj.put(field, DUMMY_IMAGE_DATA_URL);
        } else if (child.isObject() || child.isArray()) {
          filterSensitiveFields(child);
          // Bedrock Converse: bytes is inside a "source" child; update the sibling "format" to match
          if ("source".equals(field) && obj.has("format")) {
            JsonNode bytesNode = child.get("bytes");
            if (bytesNode != null && DUMMY_IMAGE_BASE64.equals(bytesNode.textValue())) {
              obj.put("format", "png");
            }
          }
        }
      }
    } else if (node.isArray()) {
      for (JsonNode item : node) {
        if (item != null && (item.isObject() || item.isArray())) {
          filterSensitiveFields(item);
        }
      }
    }
  }

  /**
   * Create a default object mapper
   * 
   * @return An object mapper
   */
  public static ObjectMapper defaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
    return mapper;
  }
}