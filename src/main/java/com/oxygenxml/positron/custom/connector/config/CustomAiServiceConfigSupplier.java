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
package com.oxygenxml.positron.custom.connector.config;

import java.util.List;

import com.oxygenxml.positron.api.connector.dto.Pair;


/**
 * Interface for supplying configuration parameters for CustomAIService.
 */
public interface CustomAiServiceConfigSupplier {
  /**
   * Get the base URL of the AI service.
   * 
   * @return The base URL.
   */
  String getBaseUrl();

  /**
   * Get the API key for the AI service.
   * 
   * @return The API key.
   */
  String getApiKey();

  /**
   * Get the headers list.
   * 
   * @return A list of headers.
   */
  List<Pair<String, String>> getHeaders();

  /**
   * Get the query parameters list.
   * 
   * @return A list of query parameters.
   */
  List<Pair<String, String>> getQueryParams();
  
  /**
   * @return <code>true</code> if the moderation is enabled, <code>false</code> otherwise.
   */
  boolean isModerationEnabled();
}
