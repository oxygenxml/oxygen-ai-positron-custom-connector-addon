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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxygenxml.positron.api.connector.AIConnectionException;
import com.oxygenxml.positron.api.connector.ProxyConnectionInfo;
import com.oxygenxml.positron.api.connector.ProxyProvider;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import ro.sync.basic.util.URLUtil;

/**
 * Utility methods
 * 
 * @author cosmin_duna
 */
public class AiServiceUtil {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AiServiceUtil.class.getName());

  /**
   * Json mapper
   */
  private static final ObjectMapper mapper =  new ObjectMapper();
  
  /**
   * Constructor.
   *
   * @throws UnsupportedOperationException when invoked.
   */
  private AiServiceUtil() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }

  
  /**
   * Calls the api and returns a Flowable of type T for streaming
   * omitting the last message ("DONE").
   *
   * @param apiCall The api call
   * @param cl      Class of type T to return
   */
  public static <T> Flow.Publisher<T> streamFlow(Call<ResponseBody> apiCall, Class<T> cl) {
    SubmissionPublisher<T> submissionPublisher = new SubmissionPublisher<>(
        Executors.newFixedThreadPool(1), Flow.defaultBufferSize());
    apiCall.enqueue(new ResponseBodyCallback<>(submissionPublisher, cl));
    return submissionPublisher;
  }
  
  

  /**
   * Calls the api, returns the response, and parses error messages if the request fails
   * 
   * @throws AIConnectionException 
   */
  public static <T> T execute(Call<T> apiCall) throws AIConnectionException {
      try {
        Response<T> response = apiCall.execute();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("<-- Received response;\n\t Code: {};", response.code());
        }
        if (!response.isSuccessful()) {
          HttpException httpException = new HttpException(response);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n\tExceptions message is: " + httpException.getMessage());
          }
          throw AiServiceUtil.processHttpException(httpException);
        }
        return response.body();
      } catch (IOException e) {
        throw new AIConnectionException(e.getMessage(), "", e);
      }  
  }

  /**
   * Process the HTTP exception resulting from AI connection
   * 
   * @param e    The HTTP exception to process
   * 
   * @return     The resulting AI connection exception
   */
  public static AIConnectionException processHttpException(HttpException e) {
    AIConnectionException aiException = new AIConnectionException(e.getMessage(), String.valueOf(e.code()), e);
    if (e.response() != null && e.response().errorBody() != null) {
      try {
        String errorBody = e.response().errorBody().string();
        AIError error = mapper.readValue(errorBody, AIError.class);
        Optional<String> errorMessage = error.getErrorMessage();
        aiException = new AIConnectionException(
            errorMessage.isPresent() ? errorMessage.get() : e.getMessage(),
            error.getErrorCode().orElse(null),
            e);
      } catch (IOException ex) {
        // couldn't parse AI error
      }
    }
    return aiException;
  }
  
  /**
   * @return <code>true</code> if the current error codes mark an Open Ai invalid API Key.
   */
  public static boolean isInvalidOpenAiApiKey(String customErrorCode, int httpErrorCode) {
    return httpErrorCode == HttpStatus.SC_UNAUTHORIZED && 
        (customErrorCode == null || "invalid_api_key".equals(customErrorCode));
  }
  
  
  /**
   * Configure proxy
   * 
   * @param builder The HTTP builder to configure
   * @param url     The URL to access.
   * @param         The proxy provider
   * 
   * @return  The configured HTTP builder
   */
  public static OkHttpClient.Builder configureProxy(OkHttpClient.Builder builder, String url, ProxyProvider proxyProvider) {
    Proxy proxy = null;
    ProxyConnectionInfo proxyInfo = proxyProvider.getProxyConnectionInfo(URLUtil.convertToURL(url));
    if (proxyInfo != null) {
      try {
        proxy = new Proxy(Proxy.Type.HTTP,
            new InetSocketAddress(InetAddress.getByName(proxyInfo.getHost()), proxyInfo.getPort()));
      } catch (UnknownHostException e) {
        LOGGER.debug(e, e);
      }
    }
    
    if(proxy != null) {
      builder = builder.proxy(proxy);
    }
    return builder;
  }
}
