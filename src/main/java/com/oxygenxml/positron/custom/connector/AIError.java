 /*
  *   Copyright [yyyy] Syncro Soft SRL
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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Error body when an Positron request fails
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AIError {

  /**
   * The error details
   */
  private ErrorDetails error;

  /**
   * Gets the error details.
   * 
   * @return The error details.
   */
  public ErrorDetails getError() {
    return error;
  }

  /**
   * Sets the error details.
   * 
   * @param error The error details to set.
   */
  public void setError(ErrorDetails error) {
    this.error = error;
  }

  /**
   * Error details
   * 
   * @author cosmin_duna
   *
   */
  static class ErrorDetails {
    /**
     * Error message
     */
    private String message;

    /**
     * Error code
     */
    private String code;
    /**
     * Gets the error message.
     * 
     * @return The error message.
     */
    public String getMessage() {
      return message;
    }

    /**
     * Sets the error message.
     * 
     * @param message The error message to set.
     */
    public void setMessage(String message) {
      this.message = message;
    }

    /**
     * @return The error code
     */
    public String getCode() {
      return code;
    }

    /**
     * Sets the error code.
     * 
     * @param code The error code to set.
     */
    public void setCode(String code) {
      this.code = code;
    }
  }

  /**
   * Get the error message.
   * 
   * @return the error message.
   */
  public Optional<String> getErrorMessage() {
    Optional<String> message = Optional.empty(); 
    if(error != null) {
      message = Optional.ofNullable(error.getMessage());
    }
    return message;
  }
  
  /**
   * Get an optional with error code
   * 
   * @return The optional with error code or empty optional.
   */
  public Optional<String> getErrorCode() {
    return Optional.ofNullable(error).map(ErrorDetails::getCode);
  }
  
}
