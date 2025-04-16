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
package com.oxygenxml.positron.custom.connector;

import java.util.List;

/**
 * An object containing a response from the moderation api
 */
public class ModerationResult {
  /**
   * A unique id assigned to this moderation.
   */
  private String id;

  /**
   * A list of moderation scores.
   */
  private List<Moderation> results;

  /**
   * @return The ID
   */
  public String getId() {
    return id;
  }

  /**
   * Set an ID
   * @param id The ID to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return The results
   */
  public List<Moderation> getResults() {
    return results;
  }

  /**
   * Set moderation results
   * @param results Moderation results
   */
  public void setResults(List<Moderation> results) {
    this.results = results;
  }
}