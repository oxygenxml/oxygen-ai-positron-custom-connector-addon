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

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import com.oxygenxml.positron.api.connector.AIConnectionException;
import com.oxygenxml.positron.api.connector.dto.Pair;

import okhttp3.Interceptor;
import okhttp3.Request.Builder;
import okhttp3.Response;

/**
 * OkHttp Interceptor that adds headers and query params
 */
public class HeadersQueryInterceptor implements Interceptor {

  /**
   * Supplier for the headers to be set on request.
   */
  private final Supplier<List<Pair<String, String>>> headersSupplier;

  /**
   * Supplier for the query params to be set on request.
   */
  private final Supplier<List<Pair<String, String>>> queryParamsSupplier;

  /**
   * Constructor
   * @param headersProvider     The provider with the headers to be added.
   * @param queryParams         The query params to add
   */
  public HeadersQueryInterceptor(Supplier<List<Pair<String, String>>> headersSupplier, Supplier<List<Pair<String, String>>> queryParamsSupplier) {
    this.headersSupplier = headersSupplier;
    this.queryParamsSupplier = queryParamsSupplier;
  }

  /**
   * Intercept and add the headers and query params to the request.
   * 
   * @param chain     The chain of interceptors
   * @return          The response
   * 
   * @throws IOException If the request fails
   */
  @Override
  public Response intercept(Chain chain) throws IOException {
    okhttp3.HttpUrl.Builder urlBuilder = chain.request().url().newBuilder();
    if(queryParamsSupplier != null && queryParamsSupplier.get() != null) {
      for (Pair<String, String> pair : queryParamsSupplier.get()) {
        urlBuilder = urlBuilder.addQueryParameter(pair.getFirst(), pair.getSecond());
      } 
    }
    
    Builder newBuilder = chain.request().newBuilder().url(urlBuilder.build());
    if(headersSupplier != null && headersSupplier.get() != null) {
      for (Pair<String, String> pair : headersSupplier.get()) {
        newBuilder = newBuilder.header(pair.getFirst(), pair.getSecond());
      } 
    }

    try {
      return chain.proceed(newBuilder.build());
    } catch (IOException e) {
      throw new IOException(e.getMessage(), new AIConnectionException(e.getMessage(), "500", e));
    }
  }
}
