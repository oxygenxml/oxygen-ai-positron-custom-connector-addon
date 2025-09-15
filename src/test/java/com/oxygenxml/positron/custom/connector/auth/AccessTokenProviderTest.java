package com.oxygenxml.positron.custom.connector.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Tests for {@link AccessTokenProvider}
 * 
 * @author cosmin_duna
 */
class AccessTokenProviderTest {

  private static final String AUTH_DOMAIN = "https://test_positron_connector.com";  // This should be a mockable URL for testing
  private static final String CLIENT_ID = "test-client-id";
  private static final String CLIENT_SECRET = "test-client-secret";
  private static final String AUTH_AUDIENCE = "test-audience";
  private static final String AUTH_SCOPE = "test-scope";
//  
  private AccessTokenProvider accessTokenProvider;
  private MockWebServer mockWebServer;

  @Mock
  private OkHttpClient mockClient;

  @Mock
  private Call mockCall;

  @Mock
  private Response mockResponse;

  @BeforeEach
  void setUp() throws IOException {
    // Initialize the mocks
    MockitoAnnotations.openMocks(this);

    // Mock the Call to return the mockResponse
    when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
    when(mockCall.execute()).thenReturn(mockResponse);
    
    accessTokenProvider = new AccessTokenProvider(new DummyProxyProvider()) {
      @Override
      okhttp3.OkHttpClient createClient(String requestUrl) {
        return mockClient;
      };
    };

      // Set environment variables for the test
      System.setProperty(AccessTokenProvider.AUTH_DOMAIN, AUTH_DOMAIN);
      System.setProperty(AccessTokenProvider.CLIENT_ID, CLIENT_ID);
      System.setProperty(AccessTokenProvider.CLIENT_SECRET, CLIENT_SECRET);
      System.setProperty(AccessTokenProvider.AUTH_AUDIENCE, AUTH_AUDIENCE);
  }
  
  @AfterEach
  void tearDown() throws Exception {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  @Test
  void testLoadAuthenticationToken_Success() throws IOException, AuthRequestException {
      // Prepare the mock response
      String jsonResponse = "{ \"access_token\": \"eyJz93a...k4laUWw\", \"token_type\": \"Bearer\", \"expires_in\": 86400 }";
      
      // Mock the Response's behavior
      when(mockResponse.isSuccessful()).thenReturn(true);
      when(mockResponse.body()).thenReturn(ResponseBody.create(MediaType.get("application/json"), jsonResponse));
      


      // Test the loadAuthenticationToken method
      accessTokenProvider.loadAuthenticationToken();

      // Verify that the access token was set correctly
      assertTrue(accessTokenProvider.getAccessToken().isPresent());
      assertEquals("eyJz93a...k4laUWw", accessTokenProvider.getAccessToken().get());
  }

  @Test
  void testLoadAuthenticationToken_Failure_AuthRequestException() {
      // Test when a required environment variable is missing
      System.clearProperty(AccessTokenProvider.CLIENT_ID);

      AuthRequestException exception = assertThrows(AuthRequestException.class, () -> {
          accessTokenProvider.loadAuthenticationToken();
      });

      assertEquals("API Key or OAuth Client Credentials Flow is not configured. Missing: POSITRON_CONNECTOR_AUTH_CLIENT_ID", exception.getMessage());
  }

  @Test
  void testLoadAuthenticationToken_Failure_HTTPError() throws IOException {
      // Mock error response (non-success status)
      when(mockResponse.isSuccessful()).thenReturn(false);
      when(mockResponse.code()).thenReturn(400);
      when(mockResponse.message()).thenReturn("Bad Request");

      // Mock the Call to return the mockResponse
      when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponse);

      // Test that an exception is thrown for an HTTP error
      AuthRequestException exception = assertThrows(AuthRequestException.class, () -> {
          accessTokenProvider.loadAuthenticationToken();
      });

      assertEquals("Auth request failed with status: 400; message: Bad Request", exception.getMessage());
  }
  
  /**
   * <p><b>Description:</b> Test that AUTH_TOKEN_URL takes precedence over AUTH_DOMAIN when both are provided</p>
   *
   * @author cosmin_duna
   *
   * @throws Exception
   */
  @Test
  void testLoadAuthenticationToken_TokenUrlTakesPrecedence() throws Exception {
      // Setup MockWebServer for token URL
      mockWebServer = new MockWebServer();
      String jsonResponse = "{ \"access_token\": \"precedence-token-value\", \"token_type\": \"Bearer\", \"expires_in\": 86400 }";
      mockWebServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader("Content-Type", "application/json")
          .setBody(jsonResponse));
      mockWebServer.start();
      
      // Create a real AccessTokenProvider
      AccessTokenProvider realAccessTokenProvider = new AccessTokenProvider(new DummyProxyProvider());
      
      // Set both environment variables - AUTH_TOKEN_URL should take precedence
      System.setProperty(AccessTokenProvider.AUTH_DOMAIN, "should-not-be-used.com"); // This should be ignored
      System.setProperty(AccessTokenProvider.AUTH_TOKEN_URL, mockWebServer.url("/custom/token/path").toString());
      System.setProperty(AccessTokenProvider.AUTH_SCOPE, AUTH_SCOPE);

      
      // Call the method under test
      realAccessTokenProvider.loadAuthenticationToken();
      
      // Verify the request
      RecordedRequest recordedRequest = mockWebServer.takeRequest();
      
      // Check the URL - should use the custom path from AUTH_TOKEN_URL
      assertEquals("/custom/token/path", recordedRequest.getPath());
      
      // Check the request body
      String requestBody = recordedRequest.getBody().readUtf8();
      assertTrue(requestBody.contains("grant_type=client_credentials"));
      assertTrue(requestBody.contains("client_id=" + CLIENT_ID));
      assertTrue(requestBody.contains("client_secret=" + CLIENT_SECRET));
      assertTrue(requestBody.contains("scope=" + AUTH_SCOPE));
      
      // Check the token was set correctly
      assertTrue(realAccessTokenProvider.getAccessToken().isPresent());
      assertEquals("precedence-token-value", realAccessTokenProvider.getAccessToken().get());
      
      // Clean up environment variables
      System.clearProperty(AccessTokenProvider.AUTH_DOMAIN);
      System.clearProperty(AccessTokenProvider.AUTH_TOKEN_URL);
  }
}
