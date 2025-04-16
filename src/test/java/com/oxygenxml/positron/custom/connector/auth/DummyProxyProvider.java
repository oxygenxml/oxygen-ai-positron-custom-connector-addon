package com.oxygenxml.positron.custom.connector.auth;

import java.net.URL;

import com.oxygenxml.positron.api.connector.ProxyConnectionInfo;
import com.oxygenxml.positron.api.connector.ProxyProvider;

public class DummyProxyProvider implements ProxyProvider{

  @Override
  public ProxyConnectionInfo getProxyConnectionInfo(URL url) {
    return null;
  }

}
