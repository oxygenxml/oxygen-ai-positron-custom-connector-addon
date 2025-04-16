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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.SubmissionPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxygenxml.positron.api.connector.AIConnectionException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * Callback to parse Server Sent Events (SSE) from raw InputStream and
 * emit the events with {@link SubmissionPublisher} to allow streaming of SSE.
 * 
 * @param <T> The type of the chunks
 */
public class ResponseBodyCallback<T> implements Callback<ResponseBody> {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBodyCallback.class.getName());

  /**
   * The publisher used to emit chunks of data.
   */
  private SubmissionPublisher<T> submissionPublisher;

  /**
   * The expected type of data (chunks).
   */
  private Class<T> dataType;

  /**
   * ObjectMapper instance for JSON deserialization.
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructor 
   * 
   * @param submissionPublisher  The publisher of chunks
   * @param dataType The expected type of data (chunks).
   */
  public ResponseBodyCallback(SubmissionPublisher<T> submissionPublisher, Class<T> dataType) {
    this.submissionPublisher = submissionPublisher;
    this.dataType = dataType;

    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }


  /**
   * Handles the HTTP response and processes the Server Sent Events (SSE).
   * 
   * @param call The HTTP call object.
   * @param response The HTTP response object.
   */
  @Override
  public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
    BufferedReader reader = null;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("<-- Received response;\n\t Code: {};", response.code());
    }

    try { // NOSONAR
      if (!response.isSuccessful()) {
        HttpException httpException = new HttpException(response);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("\n\tExceptions message is: " + httpException.getMessage());
        }

        AIConnectionException processHttpException = AiServiceUtil.processHttpException(httpException);
        String errorMesssage = processHttpException.getMessage();

        throw new AIConnectionException(errorMesssage, "", processHttpException);
      }

      InputStream in = response.body().byteStream();
      reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String line;
      String sse = null;

      StringBuilder nonSSEContent = new StringBuilder();
      boolean isSEE = false;
      while (!isPublisherCancelled() && (line = reader.readLine()) != null) {
        if(line.startsWith("event:")) {
          //Skip over claude event lines.
        } else if (line.startsWith("data:")) {
          isSEE = true;
          String data = line.substring(5).trim();
          sse = data;
        } else if (!line.isEmpty() && sse == null && !isSEE) {
          nonSSEContent.append(line);
        } else if (line.equals("") && sse != null) {
          if ("[DONE]".equals(sse)) {
            break;
          }

          submit(sse);
          sse = null;
        } else {
          throw new AIConnectionException("Invalid sse format! " + line, "500", null);
        }
      }

      String nonSseResponse = nonSSEContent.toString();
      boolean hasContentThatIsNotSSE = !isSEE && !nonSseResponse.isEmpty();
      if(hasContentThatIsNotSSE) {
        // Handle response that is valid and even if it's not SSE
        try {
          mapper.readValue(nonSseResponse, dataType);
          // This is a valid data but it is not in SSE format
          submit(nonSseResponse);
        } catch (JsonProcessingException e){
          LOGGER.debug(e.getMessage(), e);
          new AIConnectionException("Invalid sse format! " + nonSseResponse, "500", null);
        }
      }

      submissionPublisher.close();

    } catch (Throwable t) {
      onFailure(call, t);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          // do nothing
        }
      }
    }
  }

  /**
   * Submits a parsed SSE chunk to the publisher.
   * 
   * @param item The SSE chunk as a string.
   */
  private void submit(String item){
    try {
      T chunkItem = mapper.readValue(item, dataType);
      submissionPublisher.submit(chunkItem);
    } catch (Exception e) {
      submitError(e);
    }
  }

  /**
   * Checks if the publisher is cancelled.
   * 
   * @return true if the publisher is closed, false otherwise.
   */
  private boolean isPublisherCancelled(){
    return submissionPublisher != null && submissionPublisher.isClosed();
  }

  /**
   * Invoked when a network exception occurs or an unexpected exception is thrown.
   * 
   * @param call The HTTP call object.
   * @param t The exception that occurred.
   */
  @Override
  public void onFailure(Call<ResponseBody> call, Throwable t) {
    submitError(t);
  }

  /**
   * Submits an error to the publisher and closes it exceptionally.
   * 
   * @param t The exception to submit.
   */
  private void submitError(Throwable t) {
    submissionPublisher.closeExceptionally(t);
  }
}
