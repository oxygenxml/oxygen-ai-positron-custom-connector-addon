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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxygenxml.positron.api.connector.ProxyProvider;
import com.oxygenxml.positron.custom.connector.AiServiceUtil;

import okhttp3.FormBody;
import okhttp3.FormBody.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Provides functionality to obtain and manage an access token for authentication.
 * This class interacts with an OAuth2.0 provider to fetch tokens using client credentials.
 */
public class AccessTokenProvider  {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenProvider.class.getName());

  
  /**
   * Client id to use when performing authentication.
   */
  public static final String CLIENT_ID = "POSITRON_CONNECTOR_AUTH_CLIENT_ID";

  /**
   * Client secret to use when performing authentication.
   */
  public static final String CLIENT_SECRET = "POSITRON_CONNECTOR_AUTH_CLIENT_SECRET";

  /**
   * The domain to use when performing authentication.
   */
  public static final String AUTH_DOMAIN= "POSITRON_CONNECTOR_AUTH_DOMAIN";

  /**
   * The access token URL to use when performing authentication.
   */
  public static final String AUTH_TOKEN_URL= "POSITRON_CONNECTOR_AUTH_TOKEN_URL";

  /**
   * The scope to use when performing authentication.
   */
  public static final String AUTH_SCOPE= "POSITRON_CONNECTOR_AUTH_SCOPE";

  /**
   * The audience to use when performing authentication.
   */
  public static final String AUTH_AUDIENCE= "POSITRON_CONNECTOR_AUTH_AUDIENCE";
  
  /**
   * The organization to use when performing authentication.
   */
  public static final String AUTH_ORGANIZATION = "POSITRON_CONNECTOR_AUTH_ORGANIZATION";
  
  /**
   * The current access token
   */
  private Optional<String> accessToken = Optional.empty();

  /**
   * Proxy provider
   */
  private ProxyProvider proxyProvider;
  
  /**
   * Constructor
   * 
   */
  public AccessTokenProvider(ProxyProvider proxyProvider) {
    this.proxyProvider = proxyProvider;
  }
  
  
  /**
   * Retrieves the current access token, if available.
   * 
   * @return An {@link Optional} containing the access token, or empty if not set.
   */
  public Optional<String> getAccessToken() {
    return accessToken;
  }
  
  /**
   * Sets the access token.
   * 
   * @param accessToken The access token to set. Can be null to clear the token.
   */
  public void setAccessToken(String accessToken) {
    this.accessToken = Optional.ofNullable(accessToken);
  }
  
  /**
   * Loads the authentication token by making a request to the OAuth2.0 provider.
   * This method uses client credentials to fetch the token.
   * 
   * @throws AuthRequestException If any required parameter is missing or the request fails.
   */
  public void loadAuthenticationToken() throws AuthRequestException {
    LOGGER.debug("Loading access token using client credentials");
    String authDomain = getProperty(AUTH_DOMAIN);
    String accessTokenUrl = getProperty(AUTH_TOKEN_URL);
    if(authDomain == null && accessTokenUrl == null) {
      throw getAuthRequestExceptionForMissingParameter(AUTH_TOKEN_URL + " or " + AUTH_DOMAIN);
    }
    
    String clientId = getProperty(CLIENT_ID);
    if(clientId == null) {
      throw getAuthRequestExceptionForMissingParameter(CLIENT_ID);
    }
    String clientSecret = getProperty(CLIENT_SECRET);
    if(clientSecret == null) {
      throw getAuthRequestExceptionForMissingParameter(CLIENT_SECRET);
    }
    String audience = getProperty(AUTH_AUDIENCE);
    String organization = getProperty(AUTH_ORGANIZATION);
    String scope = getProperty(AUTH_SCOPE);
    
    // Create an OkHttpClient
    final String requestUrl;
    if (accessTokenUrl != null) {
      requestUrl = accessTokenUrl;
    } else {
      requestUrl = "https://" + authDomain + "/oauth/token";
    }
    OkHttpClient client = createClient(requestUrl);

    Builder formBodyBuilder = new FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", clientId)
            .add("client_secret", clientSecret);

    if(scope != null) {
      formBodyBuilder.add("scope", scope);
    }
    if(audience != null) {
      formBodyBuilder.add("audience", audience);
    }
    if(organization != null) {
      formBodyBuilder.add("organization", organization);
    }
    // Create the request
    Request request = new Request.Builder()
            .url(requestUrl)
            .post(formBodyBuilder.build())
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .build();

    // Execute the request and get the response
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        String responseContent = response.body().string();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseContent);
        setAccessToken(rootNode.path("access_token").asText());
        LOGGER.debug("Access token successfully obtained");
      } else {
        String errorMessage = "";
        if(response.body() != null) {
          errorMessage = response.body().string();
        } else {
          errorMessage = response.message();
        }
        LOGGER.debug("Auth request failed with status: " + response.code() + "; message: " + errorMessage);
        throw new AuthRequestException("Auth request failed with status: " + response.code() + "; message: " + errorMessage);
      }
    } catch (IOException e) {
      LOGGER.debug(e.getMessage());
      throw new AuthRequestException(e.getMessage());
    }
  }
  
  /**
   * Creates an exception indicating that a required parameter is missing.
   * 
   * @param paramName The name of the missing parameter.
   * @return An {@link AuthRequestException} with a descriptive message.
   */
  private static AuthRequestException getAuthRequestExceptionForMissingParameter(String paramName){
    return new AuthRequestException(
        "API Key or OAuth Client Credentials Flow is not configured. Missing: " + paramName);
  }
  
  /**
   * Creates and returns an instance of {@link OkHttpClient}.
   * 
   * @param requestUrl The URL to create the client for
   * 
   * @return A new {@link OkHttpClient} instance.
   */
  OkHttpClient createClient(String requestUrl){
    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    builder = AiServiceUtil.configureProxy(builder, requestUrl, proxyProvider);
    return builder.build();
  }
  
  /**
   * Retrieves the value of a property from the environment variables or system properties.
   * 
   * @param propertyName The name of the property to retrieve.
   * @return The value of the property, or null if not found.
   */
  private static String getProperty(String propertyName) {
    String value = null;
    
    value = System.getenv(propertyName);
    
    if(value == null) {
      value = System.getProperty(propertyName);
    }
    
    return value;
  }
}
