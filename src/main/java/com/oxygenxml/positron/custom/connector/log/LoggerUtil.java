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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class for logging
 * 
 * @author cosmin_duna
 *
 */
public class LoggerUtil {

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
   * Filter the messages from the request body because it may contain confidential data.
   * @param requestBody The request body
   * 
   * @return The filtered request body
   */
  public static String filterMessagesFromRequestBody(String requestBody) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      JsonNode root = objectMapper.readTree(requestBody);
      filterSensitiveFields(root);
      return objectMapper.writeValueAsString(root);
    } catch (Exception e) {
      // If not valid JSON, fallback to old regex (or just return original)
      return requestBody;
    }
  }

  /**
   * Recursively filter sensitive fields in the JSON tree.
   */
  private static void filterSensitiveFields(JsonNode node) {
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      // Copy field names to iterate over to avoid ConcurrentModificationException if fields are removed/modified during iteration
      List<String> fieldsToProcess = new ArrayList<>();
      obj.fieldNames().forEachRemaining(fieldsToProcess::add);

      for (String field : fieldsToProcess) {
        JsonNode child = obj.get(field);
        if ("messages".equals(field)) {
          // Replace the entire messages field with a string containing the filtered content marker
          obj.put(field, "[ **FILTERED_CONTENT** ]");
        } else if ("content".equals(field) || "input".equals(field) || "description".equals(field) 
        || "system".equals(field)) {
          // Replace the content/input/description/system field with the filtered content marker
          obj.put(field, " **FILTERED_CONTENT** ");
        } else {
          // Only recurse if the child is an object or array. This avoids processing primitive values.
          if (child != null && (child.isObject() || child.isArray())) {
            filterSensitiveFields(child);
          }
        }
      }
    } else if (node.isArray()) {
      // Iterate through array elements. If an element is an object or array, recurse into it.
      for (JsonNode item : node) {
        if (item != null && (item.isObject() || item.isArray())) {
          filterSensitiveFields(item);
        }
      }
    }
  }
  
}
