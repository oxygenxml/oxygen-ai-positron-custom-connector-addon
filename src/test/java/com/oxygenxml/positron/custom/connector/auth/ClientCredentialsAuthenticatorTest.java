package com.oxygenxml.positron.custom.connector.auth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Response.Builder;
import okhttp3.Route;

class ClientCredentialsAuthenticatorTest {

  @Mock
  private Route mockRoute;

  private ClientCredentialsAuthenticator tokenAuthenticator;

  private Builder responseBuilder;

  private String tokenSetOnLoad = null;
  
  @BeforeEach
  void setUp() {
    AccessTokenProvider tokenProvider = new AccessTokenProvider(null) {
      @Override
      public void loadAuthenticationToken() throws AuthRequestException {
        setAccessToken(tokenSetOnLoad);
      }
    };
      MockitoAnnotations.openMocks(this);
      tokenAuthenticator = new ClientCredentialsAuthenticator(tokenProvider);
      
      okhttp3.Request.Builder requestBuilder = new Request.Builder();
      requestBuilder.url("https://testRequest.com");
      responseBuilder = new Response.Builder();
      responseBuilder.request(requestBuilder.build());
      responseBuilder.code(401);
      responseBuilder.protocol(Protocol.HTTP_2);
      responseBuilder.message("test");
  }


    @Test
    void testAuthenticate_WhenTokenIsNotAvailable() throws IOException {
        // Simulate no token available
        tokenSetOnLoad = null;
        
        // Call the method
        Request authenticatedRequest = tokenAuthenticator.authenticate(mockRoute, responseBuilder.build());

        // Validate that the response is null since no token is available
        assertNull(authenticatedRequest);
    }


    @Test
    void testAuthenticate_SuccessAfterTokenLoad() throws IOException, AuthRequestException {
      tokenSetOnLoad = "newAccessToken";

      // Call the method to authenticate
      Request authenticatedRequest = tokenAuthenticator.authenticate(mockRoute, responseBuilder.build());

      // Validate that the "Authorization" header has the new token
      assertNotNull(authenticatedRequest);
      assertEquals("Bearer newAccessToken", authenticatedRequest.header("Authorization"));
    }
}
