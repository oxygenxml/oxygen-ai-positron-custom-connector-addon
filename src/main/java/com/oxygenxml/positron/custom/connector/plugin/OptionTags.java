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
package com.oxygenxml.positron.custom.connector.plugin;

/**
 * Collection of keys for the plugin options.
 */
public class OptionTags {
  
  /**
    * Constructor.
    *
    * @throws UnsupportedOperationException when invoked.
    */
  protected OptionTags() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }
  
  /**
   * Prefix for all the options.
   */
  private static final String OPTION_PREFIX = "oxygen.positron.plugin.";
  /**
   * The option for connection read timeout.
   */
  public static final String CONNECTION_READ_TIMEOUT = OPTION_PREFIX + "oxygen.positron.plugin.connection.read.timeout";
}
