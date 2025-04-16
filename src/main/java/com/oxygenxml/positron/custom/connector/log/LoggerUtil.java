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
    String ret = requestBody.replaceAll("\"input\"\\s*:\\s*\"[^\"]*\"", "\"input\":\" **FILTERED_CONTENT** \"");
    ret = ret.replaceAll("\"messages\"\\s*:\\s*\\[[^\\]]*\\]", "\"messages\":[ **FILTERED_CONTENT** ]");
    return ret.replaceAll("\"content\"\\s*:\\s*\"[^\"]*\"", "\"content\":\" **FILTERED_CONTENT** \"");
  }
}
