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
package com.oxygenxml.positron.custom.connector.auth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Authenticator that adds an authorization token header
 */
public class ClientCredentialsAuthenticator implements Authenticator {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientCredentialsAuthenticator.class.getName());

  /**
   * The header for authorization
   */
  private static final String AUTHORIZATION_HEADER = "Authorization";

  /**
   * Token provider that supplies and manages access tokens.
   */
  private final AccessTokenProvider tokenProvider;

  /**
   * Constructor
   * 
   * @param tokenProvider  Token provider
   */
  public ClientCredentialsAuthenticator(AccessTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  /**
   * Authenticates the request by adding an authorization token header.
   *
   * @param route    The route for the request.
   * @param response The response received.
   * @return A new request with the authorization token header, or null if the
   *         maximum number of retries is reached.
   * @throws IOException If an I/O error occurs during authentication.
   */
  @Override
  public Request authenticate(Route route, Response response) throws IOException {
    if (responseCount(response) >= 2) {
      return null;
    }

    if (!tokenProvider.getAccessToken().isPresent()
        || response.request().header(AUTHORIZATION_HEADER) == null
        || response.code() == 401) {
      try {
        tokenProvider.loadAuthenticationToken();
      } catch (AuthRequestException e) {
        LOGGER.error(e.getMessage(), e);
        throw new IOException(e.getMessage(), e);
      }
    }

    if (tokenProvider.getAccessToken().isPresent()) {
      return response.request().newBuilder()
          .header(AUTHORIZATION_HEADER, "Bearer " + tokenProvider.getAccessToken().get())
          .build();
    } else {
      return null;
    }
  }

  
  /**
   * Counts the number of prior responses.
   *
   * @param response The response to count prior responses for.
   * @return The number of prior responses, up to a maximum of 3.
   */
  private static int responseCount(Response response) {
    int result = 1;
    while (result <= 3) {
      response = response.priorResponse();
      if(response == null) {
        break;
      } else {
        result++;
      }
    }
    return result;
  }

}
