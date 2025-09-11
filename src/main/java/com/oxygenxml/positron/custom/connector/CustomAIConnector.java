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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.oxygenxml.positron.api.connector.AIConnector;
import com.oxygenxml.positron.api.connector.AIService;
import com.oxygenxml.positron.api.connector.ProxyProvider;
import com.oxygenxml.positron.api.connector.dto.CompletionRequest;
import com.oxygenxml.positron.api.connector.dto.Message;
import com.oxygenxml.positron.api.connector.dto.ModelDescriptor;
import com.oxygenxml.positron.api.connector.dto.Pair;
import com.oxygenxml.positron.api.connector.dto.RoleType;
import com.oxygenxml.positron.api.connector.param.CheckBoxConnectorParam;
import com.oxygenxml.positron.api.connector.param.ConnectorParamBase;
import com.oxygenxml.positron.api.connector.param.KeyValueTableConnectorParam;
import com.oxygenxml.positron.api.connector.param.ModelsComboConnectorParam;
import com.oxygenxml.positron.api.connector.param.PasswordTextFieldConnectorParam;
import com.oxygenxml.positron.api.connector.param.TextFieldConnectorParam;
import com.oxygenxml.positron.custom.connector.auth.AccessTokenProvider;
import com.oxygenxml.positron.custom.connector.config.CustomAiServiceConfigSupplier;

/**
 * The AI connector for custom AI service
 * 
 * @author cosmin_duna
 */
public class CustomAIConnector extends AIConnector {
  /**
   * The default model
   */
  private static final String DEFAULT_MODEL = "gpt-4.1";

  /**
   * OpenAI connector ID
   */
  public static final String AI_CONNECTOR_ID = "custom-ai-service";
  
  /**
   * The parameter identifier for the OpenAI KEY
   */
  public static final String AI_KEY_PARAM_ID = "ai_key_param";
  
  /**
   * The parameter identifier for the base URL
   */
  public static final String BASE_URL_PARAM_ID = "base_URL_param";

  /**
   * The parameter identifier for the extra headers parameters.
   */
  public static final String EXTRA_HEADERS_PARAM_ID = "extra_headers_param";
  
  /**
   * The parameter identifier for the extra headers parameters.
   */
  public static final String EXTRA_QUERY_PARAM_ID = "extra_query_params";
  
  /**
   * The parameter identifier for the AI model
   */
  public static final String MODEL_PARAM_ID = "model_param";
  
  /**
   * The parameter identifier for enabling text moderation
   */
  public static final String ENABLE_TEXT_MODERATION_PARAM_AI = "enable_text_moderation_param";
  
  /**
   * The parameter identifier for allowing streaming.
   */
  public static final String ALLOW_STREAMING_PARAM_ID = "allow_streaming_param";
  
  /**
   * Pattern for reasoning models
   */
  private static final Pattern REASONING_MODEL_PATTERN = Pattern.compile("^o[\\d]+");
  
  /**
   * @see AIConnector#getParametersList()
   */
  @Override
  public List<ConnectorParamBase> getParametersList() {
    List<ConnectorParamBase> params = new ArrayList<>();

    params.add(new TextFieldConnectorParam(BASE_URL_PARAM_ID, "Address:", null)
        .setDefaultValue("https://api.openai.com/v1/"));
    
    final String apiKeyExtraInfo = new StringBuilder()
        .append("For OAuth Client Credentials authentication, ensure the following environment variables or system properties are set:\n")
        .append("    - ").append(AccessTokenProvider.AUTH_DOMAIN).append( ": The domain for the authorization server (e.g., 'example-123abc.us.auth0.com').\n")
        .append("    - ").append(AccessTokenProvider.CLIENT_ID).append( ": The ID of the requesting client.\n")
        .append("    - ").append(AccessTokenProvider.CLIENT_SECRET).append( ": The secret of the client.\n")
        .append("    - ").append(AccessTokenProvider.AUTH_AUDIENCE).append( ": The audience for the token, typically the API or service you're accessing. Optional.\n")
        .append("    - ").append(AccessTokenProvider.AUTH_ORGANIZATION).append( ": The organization name or identifier. Optional.\n")
        .append("\n")
        .toString();
    
    params.add(new PasswordTextFieldConnectorParam(AI_KEY_PARAM_ID, "API key:", null)
        .setInfo("If you do not specify an API key, the environment variables or system properties will be used to authenticate using OAuth Client Credentials Flow.")
        .setExtraInfo(apiKeyExtraInfo));
    
    params.add(new ModelsComboConnectorParam(MODEL_PARAM_ID, "Model:", "Choose the model", new Supplier<List<ModelDescriptor>>() {
      @Override
      public List<ModelDescriptor> get() {
        List<ModelDescriptor> models = new ArrayList<>();
        models.add(new ModelDescriptor("gpt-5", "GPT 5",  "Latest-generation flagship model designed for complex reasoning and high-accuracy tasks."));
        models.add(new ModelDescriptor("gpt-5-mini", "GPT-5 Mini",  "Smaller GPT-5 variant optimized for cost and speed while maintaining strong quality for common tasks."));
        models.add(new ModelDescriptor("gpt-5-nano", "GPT-5 Nano",  "The fastest and most cost-effective GPT-5 variant for lightweight tasks."));
        
        models.add(new ModelDescriptor(DEFAULT_MODEL, "GPT 4.1", "Smartest non-reasoning model. It excels at instruction following and tool calling, with broad knowledge across domains.")); 
        models.add(new ModelDescriptor("gpt-4.1-mini", "GPT-4.1 Mini", "Smaller, faster version of GPT-4.1")); 
        models.add(new ModelDescriptor("gpt-4.1-nano", "GPT-4.1 Nano", "GPT-4.1 nano is the fastest, most cost-effective GPT-4.1 model")); 

        return models;
      }
    }).setDefaultValue(DEFAULT_MODEL));
    
    
    params.add(new CheckBoxConnectorParam(
        ENABLE_TEXT_MODERATION_PARAM_AI,
        "Enable text moderation",
        "When enabled, this option applies moderation to both the input text sent to the AI service and the response received from the AI service. "
        + "When disabled, no moderation is performed on either the request or the response",
        Boolean.TRUE));
    
    params.add(new CheckBoxConnectorParam(
        ALLOW_STREAMING_PARAM_ID,
        "Enable streaming",
        "When selected, streaming will be enabled for the current AI connector",
        Boolean.TRUE));
    
    params.add(new KeyValueTableConnectorParam(
        EXTRA_QUERY_PARAM_ID,
        "Extra query parameters:",
        null));
    
    params.add(new KeyValueTableConnectorParam(
        EXTRA_HEADERS_PARAM_ID,
        "Extra headers:",
        null));
    
    return params;
  }

  /**
   * @see AIConnector#getConnectorId()
   */
  @Override
  public String getConnectorId() {
    return AI_CONNECTOR_ID;
  }

  /**
   * @see AIConnector#getConnectorName()
   */
  @Override
  public String getConnectorName() {
    return "Custom AI Service";
  }

  /**
   * @see AIConnector#createAIService(ProxyProvider, int)
   */
  @Override
  public AIService createAIService(ProxyProvider proxyProvider, int timeout) {
    Map<String, Object> resolvedParameters = getResolvedParameters();
    return new CustomAIService(new CustomAiServiceConfigSupplier() {
      @Override
      public String getBaseUrl() {
        return String.valueOf(resolvedParameters.get(BASE_URL_PARAM_ID));
      }
      
      @Override
      public String getApiKey() {
        return String.valueOf(resolvedParameters.get(AI_KEY_PARAM_ID));
      }
      
      @Override
      public List<Pair<String, String>> getHeaders() {
        return (List<Pair<String, String>>)resolvedParameters.get(EXTRA_HEADERS_PARAM_ID);
      }
      
      @Override
      public List<Pair<String, String>> getQueryParams() {
        return (List<Pair<String, String>>)resolvedParameters.get(EXTRA_QUERY_PARAM_ID);
      }

      @Override
      public boolean isModerationEnabled() {
        return Boolean.parseBoolean(
            String.valueOf(resolvedParameters.get(ENABLE_TEXT_MODERATION_PARAM_AI)));
      }
    }, proxyProvider, timeout);
  }

  /**
   * Enrich the given completion request by incorporating connector parameters before it is executed.
   *
   * @param request the completion request to be enriched
   * 
   * @return The enriched completion request, updated with additional parameters
   */
  @Override
  public CompletionRequest configureCompletionRequest(CompletionRequest request) {
    request = super.configureCompletionRequest(request);

    boolean isStreamingAllowed =  Boolean.parseBoolean(
        String.valueOf(getResolvedParameters().get(ALLOW_STREAMING_PARAM_ID)));
    if(request.getStream() != null && request.getStream().booleanValue() && !isStreamingAllowed) {
      request.setStream(false);
    }

    Map<String, Object> resolvedParameters = getResolvedParameters();
    if(request.getModel() == null) {
      Object model = resolvedParameters.get(MODEL_PARAM_ID);
      if(model != null && !String.valueOf(model).isEmpty()) {
        request.setModel(String.valueOf(model));
      } else {
        request.setModel(DEFAULT_MODEL);
      }
    }
    
    processRequestTakingAccountOfReasoningModel(request);
    return request;
  }
  
  /**
   * Update the completion request taking account of reasoning properties
   * 
   * Some features like the following are not supported by reasoning models
   * - system message
   * - temperature
   * - max tokens
   * 
   * Also some parameters are specific to reasoning models and should be excluded for other models
   * 
   * @param completionRequest The completion request
   */
  private static void processRequestTakingAccountOfReasoningModel(CompletionRequest completionRequest) {
    String model = completionRequest.getModel();
    boolean isReasoningModel = model.startsWith("gpt-5") || REASONING_MODEL_PATTERN.matcher(model).find();
    if(isReasoningModel) {
      // Translate System message
      Message message = completionRequest.getMessages().get(0);
      if(message.getRole() == RoleType.SYSTEM) {
        completionRequest.getMessages().set(
            0,
            new Message(
                RoleType.DEVELOPER,
                message.getContent()));
      }
      
      // Currently unsupported API parameters: temperature
      completionRequest.setTemperature(null);
      
      // Max tokens is not supported for reasoning models
      completionRequest.setMaxTokens(null);
    } else {
      // Ignore max_completion_tokens and reasoning_effort on models that are not from o1 family
      completionRequest.setMaxCompletionTokens(null);
      completionRequest.setReasoningEffort(null);
    }
    
  }
}
