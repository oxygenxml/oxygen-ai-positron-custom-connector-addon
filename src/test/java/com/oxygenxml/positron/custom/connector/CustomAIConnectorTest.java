package com.oxygenxml.positron.custom.connector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.oxygenxml.positron.api.connector.dto.CompletionRequest;
import com.oxygenxml.positron.api.connector.dto.CompletionResponse;
import com.oxygenxml.positron.api.connector.dto.Message;
import com.oxygenxml.positron.api.connector.dto.MessageTextContent;
import com.oxygenxml.positron.api.connector.dto.Pair;
import com.oxygenxml.positron.api.connector.dto.RoleType;
import com.oxygenxml.positron.custom.connector.auth.DummyProxyProvider;

import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
/**
 * Tests for {@link CustomAIConnector}
 */
public class CustomAIConnectorTest {
  private MockWebServer server = new MockWebServer();
  private static final int MOCK_WEBSERVER_PORT = 8074;
  
  @BeforeEach
  void setUp() throws IOException {
    server.enqueue(new MockResponse().setBody("{\n" + 
        "    \"choices\": [\n" + 
        "        {\n" + 
        "            \"index\": 0,\n" + 
        "            \"message\": {\n" + 
        "                \"role\": \"assistant\",\n" + 
        "                \"content\": \"Desigur! Iata o gluma:\"\n" +
        "            },\n" + 
        "            \"finish_reason\": \"stop\"\n" + 
        "        }\n" + 
        "    ]\n" + 
        "}"));
    server.start(MOCK_WEBSERVER_PORT);
  }
 
  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }
  
  
  /**
   * <p><b>Description:</b> Test that AI Connector parameters are set properly on request</p>
   * <p><b>Bug ID:</b> OPA-1239</p>
   *
   * @author cosmin_duna
   *
   * @throws Exception
   */
  @Test
  void testAIConnectorParameters() throws Exception {
    
    CustomAIConnector customAIConnector = new CustomAIConnector();
        
    Map<String, Object> resolvedParameter = new HashMap<>();
    resolvedParameter.put(CustomAIConnector.BASE_URL_PARAM_ID, server.url("/").toString());
    resolvedParameter.put(CustomAIConnector.AI_KEY_PARAM_ID, "apiKey");
    resolvedParameter.put(CustomAIConnector.ENABLE_TEXT_MODERATION_PARAM_AI, false);
    
    List<Pair<String, String>> extraHeaders = new ArrayList<Pair<String, String>>();
    extraHeaders.add(new Pair<String, String>("extraHeader1", "val"));
    resolvedParameter.put(CustomAIConnector.EXTRA_HEADERS_PARAM_ID, extraHeaders);
    
    List<Pair<String, String>> queryParams = new ArrayList<Pair<String, String>>();
    queryParams.add(new Pair<String, String>("queryParam1", "queryParamVal1"));
    resolvedParameter.put(CustomAIConnector.EXTRA_QUERY_PARAM_ID, queryParams);
    customAIConnector.setResolvedParameters(resolvedParameter);
    
    CustomAIService aiService = (CustomAIService) customAIConnector.createAIService(new DummyProxyProvider(), 0);
    
    CompletionRequest completionRequest = new CompletionRequest();
    completionRequest.setModel("gpt-3.5");
    ArrayList<Message> messages = new ArrayList<Message>();
    messages.add(
        new Message(
            RoleType.SYSTEM,
            new MessageTextContent("Act as a dev")));
    messages.add(
        new Message(
            RoleType.USER,
            new MessageTextContent("Spune o gluma")));

    completionRequest.setMessages(messages);
    CompletionResponse completion = aiService.getCompletion(completionRequest);
    assertEquals("Desigur! Iata o gluma:", completion.getChoices().get(0).getCompletionText());
    
    RecordedRequest request1 = server.takeRequest();
    assertEquals(server.url("/").toString() + "chat/completions?queryParam1=queryParamVal1",  request1.getRequestUrl().toString());
    assertEquals("{\"model\":\"gpt-3.5\",\"messages\":"
        + "[{\"role\":\"system\",\"content\":\"Act as a dev\"},{\"role\":\"user\",\"content\":\"Spune o gluma\"}]}",
        request1.getBody().readUtf8());
    Headers headers = request1.getHeaders();
    assertEquals("Bearer apiKey", headers.get("authorization"));
    assertEquals("val", headers.get("extraHeader1"));
  }

  /**
   * <p><b>Description:</b> Test that AI Connector parameters are set properly on request when URL contains its own path</p>
   * <p><b>Bug ID:</b> OPA-2341</p>
   *
   * @author radu_coravu
   *
   * @throws Exception
   */
  @Test
  void testAIConnectorParametersCustomURLPath() throws Exception {
    
    CustomAIConnector openAIConnector = new CustomAIConnector();
        
    Map<String, Object> resolvedParameter = new HashMap<>();
    resolvedParameter.put(CustomAIConnector.BASE_URL_PARAM_ID, server.url("/").toString() + "abc/def/");
    resolvedParameter.put(CustomAIConnector.AI_KEY_PARAM_ID, "apiKey");
    resolvedParameter.put(CustomAIConnector.ENABLE_TEXT_MODERATION_PARAM_AI, false);
    
    List<Pair<String, String>> queryParams = new ArrayList<Pair<String, String>>();
    queryParams.add(new Pair<String, String>("queryParam1", "queryParamVal1"));
    resolvedParameter.put(CustomAIConnector.EXTRA_QUERY_PARAM_ID, queryParams);
    
    openAIConnector.setResolvedParameters(resolvedParameter);
    
    CustomAIService aiService = (CustomAIService) openAIConnector.createAIService(new DummyProxyProvider(), 0);
    
    CompletionRequest completionRequest = new CompletionRequest();
    completionRequest.setModel("gpt-3.5");
    ArrayList<Message> messages = new ArrayList<Message>();
    messages.add(
        new Message(
            RoleType.SYSTEM,
            new MessageTextContent("Act as a dev")));
    messages.add(
        new Message(
            RoleType.USER,
            new MessageTextContent("Spune o gluma")));
  
    completionRequest.setMessages(messages);
    aiService.getCompletion(completionRequest);
    
    RecordedRequest request1 = server.takeRequest();
    assertEquals(server.url("/").toString() + "abc/def/chat/completions?queryParam1=queryParamVal1",  request1.getRequestUrl().toString());
  }
  
}
