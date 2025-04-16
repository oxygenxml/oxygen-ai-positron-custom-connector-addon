package com.oxygenxml.positron.custom.connector.plugin;

import java.util.ArrayList;
import java.util.List;

import com.oxygenxml.positron.api.connector.AIConnector;
import com.oxygenxml.positron.custom.connector.CustomAIConnector;

import ro.sync.exml.plugin.ai.AIConnectorsPluginExtension;

/**
 * Plugin extension for AI connectors.
 */
public class PositronConnectorsPluginExtension implements AIConnectorsPluginExtension {

  /**
   * Get the custom/external AI connectors to external AI services.
   * 
   * @return The custom AI connectors to external AI services.
   */
  @Override
  public List<AIConnector> getExternalAIConnectors() {
    List<AIConnector> connectors = new ArrayList<>();
    CustomAIConnector customConnector = new CustomAIConnector();
    connectors.add(customConnector);
    return connectors;
  };
}