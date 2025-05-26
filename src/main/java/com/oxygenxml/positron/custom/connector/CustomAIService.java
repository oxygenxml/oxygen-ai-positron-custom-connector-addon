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
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.oxygenxml.positron.api.connector.AIConnectionException;
import com.oxygenxml.positron.api.connector.AIService;
import com.oxygenxml.positron.api.connector.ProxyProvider;
import com.oxygenxml.positron.api.connector.dto.CompletionChunk;
import com.oxygenxml.positron.api.connector.dto.CompletionRequest;
import com.oxygenxml.positron.api.connector.dto.CompletionResponse;
import com.oxygenxml.positron.api.connector.dto.ModerationRequest;
import com.oxygenxml.positron.custom.connector.auth.AccessTokenProvider;
import com.oxygenxml.positron.custom.connector.auth.ClientCredentialsAuthenticator;
import com.oxygenxml.positron.custom.connector.auth.PreemptiveAuthInterceptor;
import com.oxygenxml.positron.custom.connector.config.CustomAiServiceConfigSupplier;
import com.oxygenxml.positron.custom.connector.config.HeadersQueryInterceptor;
import com.oxygenxml.positron.custom.connector.log.RequestLoggingInterceptor;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * The OpenAI service
 * 
 * @author cosmin_duna
 */
public class CustomAIService implements AIService {
  /**
   * The header for authorization
   */
  private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomAIService.class.getName());
  
  /**
   * OpenAI API
   */
  private OpenAiApi api = null;

  /**
  * The default request timeout in miliseconds.
  */
  protected static final int DEFAULT_REQUEST_TIMEOUT = 600000;

  /**
   * <code>true</code> when moderation should be applied, <code>false</code> otherwise
   */
  private boolean shouldApplyModeration;

  /**
   * Proxy provider
   */
  private ProxyProvider proxyProvider;

  /**
   * The timeout to use
   */
  private int timeout;

  /**
   * Constructor
   * @param proxyProvider The proxy provider to retrieve proxy configuration.
   * @param timeout       The timeout value for the service.
   * 
   * @param configSupplier The configuration supplier for the AI service
   */
  public CustomAIService(CustomAiServiceConfigSupplier configSupplier, ProxyProvider proxyProvider, int timeout) {
    this.proxyProvider = proxyProvider;
    this.timeout = timeout;
    this.shouldApplyModeration = configSupplier.isModerationEnabled();
    
    String baseUrl = configSupplier.getBaseUrl();
    OkHttpClient client = createServiceHttpClient(configSupplier);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl).client(client)
        .addConverterFactory(JacksonConverterFactory.create(mapper)).build();

    api = retrofit.create(OpenAiApi.class);
  }

  /**
   * @see AIService#getCompletionFlux(CompletionRequest)
   */
  @Override
  public Flow.Publisher<CompletionChunk> getCompletionFlux(CompletionRequest completionRequest) {
    return AiServiceUtil.streamFlow(api.createChatCompletionStream(completionRequest), CompletionChunk.class);
  }

  /**
   * @see AIService#getCompletion(CompletionRequest)
   */
  @Override
  public CompletionResponse getCompletion(CompletionRequest completionRequest) throws AIConnectionException {
    return AiServiceUtil.execute(api.createChatCompletion(completionRequest));
  }

  /**
   * @see AIService#isRequiringApplyingModeration()
   */
  @Override
  public boolean isRequiringApplyingModeration() {
    return shouldApplyModeration;
  }

  /**
   * @see AIService#applyModeration(ModerationRequest)
   */
  @Override
  public boolean applyModeration(ModerationRequest moderationRequest) throws AIConnectionException {
     boolean isFlagged = false;
    List<Moderation> results = AiServiceUtil.execute(api.createModeration(moderationRequest)).getResults();
    if(results != null) {
      for (Moderation moderation : results) {
        if(moderation.isFlagged()) {
          isFlagged = true;
          break;
        }
      }
    }
    return isFlagged;
  }

  /**
   * Create the HTTP client
   * 
   * @param configSupplier The configuration supplier for the AI service
   * 
   * @return The created HTTP client
   */
  private OkHttpClient createServiceHttpClient(CustomAiServiceConfigSupplier configSupplier) {
    String baseUrl = configSupplier.getBaseUrl();
    String token = configSupplier.getApiKey();

    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    builder = AiServiceUtil.configureProxy(builder, baseUrl, proxyProvider);

    if (token != null && !token.isBlank() && !token.equals("null")) {
      LOGGER.debug("Found API key in preferences page");
      builder = builder.addInterceptor(chain -> {
        Request request = chain.request();
        Request newRequest = request.newBuilder().addHeader(AUTHORIZATION_HEADER_NAME, "Bearer " + token).build();
        return chain.proceed(newRequest);
      });
    } else {
      AccessTokenProvider accessTokenProvider = new AccessTokenProvider(proxyProvider);
      builder = builder.addInterceptor(new PreemptiveAuthInterceptor(accessTokenProvider));
      builder.authenticator(new ClientCredentialsAuthenticator(accessTokenProvider));
    }

    if (configSupplier.getHeaders() != null || configSupplier.getQueryParams() != null) {
      builder.addInterceptor(new HeadersQueryInterceptor(() -> configSupplier.getHeaders(), () -> configSupplier.getQueryParams()));
    }

    if(LOGGER.isDebugEnabled()) {
      builder = builder.addInterceptor(new RequestLoggingInterceptor(LOGGER));
    }

    return builder.connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
        .readTimeout(timeout, TimeUnit.MILLISECONDS).build();
  }
}
