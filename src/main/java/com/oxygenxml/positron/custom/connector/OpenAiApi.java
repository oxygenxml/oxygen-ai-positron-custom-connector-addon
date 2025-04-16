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

import com.oxygenxml.positron.api.connector.dto.CompletionRequest;
import com.oxygenxml.positron.api.connector.dto.CompletionResponse;
import com.oxygenxml.positron.api.connector.dto.ModerationRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

/**
 * OpenAI API 
 * 
 * @author cosmin_duna
 */
public interface OpenAiApi {

  /**
   * Create a chat completion request
   * 
   * @param request The request body
   * 
   * @return The chat completion response
   */
  @POST("chat/completions")
  Call<CompletionResponse> createChatCompletion(@Body CompletionRequest request);

  /**
   * Create a chat completion streaming request
   * 
   * @param request The request body
   * 
   * @return The call to the chat completion streaming request.
   */
  @Streaming
  @POST("chat/completions")
  Call<ResponseBody> createChatCompletionStream(@Body CompletionRequest request);

  /**
   * Create a moderation request
   * 
   * @param request The request body
   * 
   * @return The moderation response
   */
  @POST("moderations")
  Call<ModerationResult> createModeration(@Body ModerationRequest request);
}
