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

import java.io.IOException;

import org.slf4j.Logger;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

/**
 * Interceptor that logs request and response
 * 
 * @author cosmin_duna
 */
public class RequestLoggingInterceptor implements Interceptor {

  /**
   * The logger where to log the requests and responses 
   */
  private Logger log;
  
  /**
   * Constructor
   * 
   * @param logger The logger where to log the requests and responses 
   */
    public RequestLoggingInterceptor(Logger logger) {
      this.log = logger;
    }
    /**
     * Intercept and adds logging on request and response
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();
      if(log.isDebugEnabled()) {
        try (Buffer requestBuffer = new Buffer()) {
          if (request.body() != null) {
            request.body().writeTo(requestBuffer);
          }
          log.debug("--> Sending request to: {};\n\t Request body: {};\n\t Headers names: {} ", request.url(),
              LoggerUtil.filterMessagesFromRequestBody(requestBuffer.readUtf8()), request.headers().names());

        }
      }

      return chain.proceed(request);
    }
    
}