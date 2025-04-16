package com.oxygenxml.positron.custom.connector.auth;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor that adds makes a preemptive authentication 
 * when Authorization header is not available and set it.
 */
public class PreemptiveAuthInterceptor implements Interceptor {

  /**
   * The header for authorization
   */
  private static final String AUTHORIZATION_HEADER = "Authorization";
  
  /**
   * Token provider that supplies and manages access tokens.
   */
  private final AccessTokenProvider tokenProvider;
  
  /**
   * Constructor
   * 
   * @param tokenProvider  Token provider
   */
  public PreemptiveAuthInterceptor(AccessTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }
  
  /**
   * Intercept the request and make a  preemptive authentication
   */
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if(request.header(AUTHORIZATION_HEADER) == null) {
      if(tokenProvider.getAccessToken().isEmpty()) {
        try {
          tokenProvider.loadAuthenticationToken();
        } catch (AuthRequestException e) {
          throw new IOException(e.getMessage(), e);
        }
      }
      
      if(tokenProvider.getAccessToken().isPresent()) {
        request = request.newBuilder().addHeader(
            AUTHORIZATION_HEADER, "Bearer " + tokenProvider.getAccessToken().get()).build();
      }
    } 
    return chain.proceed(request);
  }

}
