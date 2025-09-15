# Oxygen AI Positron Custom Connector

This add-on allows you to connect the Oxygen AI Positron Assistant Enterprise plugin with AI services that expose a REST API, similar to OpenAI's chat-completion API. Unlike the built-in Open AI connector, this add-on supports the OAuth Client Credentials Flow for authentication and gives you more flexibility by letting you set query parameters.

## Compatibility

The add-on is compatible with Oxygen XML Editor/Author/Developer version 27.1 or newer. 

## How to install

1. Go to **Help->Install new add-ons** to open an add-on selection dialog box.
2. Enter or paste https://www.oxygenxml.com/InstData/Addons/default/updateSite.xml in the **Show add-ons from** field.
3. Select the **Oxygen AI Positron Custom Connector** add-on and click **Next**.
4. Read the end-user license agreement. Then select the **I accept all terms of the end-user license agreement** option and click **Finish**.
5. Restart the application. 

Result: The **Custom AI Service** will be available into the AI Connector combo menu from the "Plugins / Oxygen AI Positron Assistant Enterprise / AI Service Configuration" preferences page of the Oxygen AI Positron Assistant Enterprise add-on.

The add-on can also be installed using the following alternative installation procedure:
1. Go to the [Releases page](https://www.oxygenxml.com/InstData/Addons/default/com/oxygenxml/oxygen-ai-positron-custom-connector-addon) and download the `oxygen-ai-positron-custom-connector-addon-{version}-plugin.jar` file.
2. Unzip it inside `{oXygenInstallDir}/plugins`. Make sure you don't create any intermediate folders. After unzipping the archive, the file system should look like this: `{oXygenInstallDir}/plugins/oxygen-ai-positron-custom-connector-addon-x.y.z`, and inside this folder, there should be a `plugin.xml`file.

## How to use
When the **Custom AI Service** connector is selected in the "AI Service Configuration" preferences page of the Oxygen AI Positron Assistant Enterprise add-on the following options are available to configure the connector: 

* Address: The web address of the Custom AI service. By default is: https://api.openai.com/v1/

* API key: The API key necessary to work with the connector. If you do not specify an API key, the add-on will try to use environment variables to authenticate using OAuth Client Credential flow. These are the env variables that should be specified: 
    * POSITRON_CONNECTOR_AUTH_DOMAIN: The domain for the authorization server (e.g., 'example-123abc.us.auth0.com'). 
    This is not required if POSITRON_CONNECTOR_AUTH_REQUEST_URL is defined.
    * POSITRON_CONNECTOR_AUTH_CLIENT_ID: The ID of the requesting client.
    * POSITRON_CONNECTOR_AUTH_CLIENT_SECRET: The secret of the client.
    * POSITRON_CONNECTOR_AUTH_AUDIENCE: The audience for the token, typically the API or service you're accessing. Optional.
    * POSITRON_CONNECTOR_AUTH_ORGANIZATION: The organization name or identifier. Optional.
    * POSITRON_CONNECTOR_AUTH_SCOPE: The OAuth 2.0 scope to request when generating the access token. Optional.
    * POSITRON_CONNECTOR_AUTH_TOKEN_URL: Full URL to the token endpoint used to obtain the access token. When this variable is defined, it overrides the default behavior and is used directly for the OAuth token request. Example: *https://your-okta-domain.com/oauth2/abc123/v1/token*.
    If this variable is not set, the request URL is constructed automatically using the following pattern: *https://<POSITRON_CONNECTOR_AUTH_DOMAIN>/oauth/token*
    
    
Here you can find more information about how you can find these values in Auth0: [Auth0 Client Credentials Flow Parameters](https://auth0.com/docs/get-started/authentication-and-authorization-flow/client-credentials-flow/call-your-api-using-the-client-credentials-flow#parameters)
* Model: The model to use.
* Enable text moderation: This setting applies moderation to both the input text sent to the AI service and the response received from the AI service. It is enabled by default. 
By default, when executing an action using this connector, three requests are made:

  1. A moderation on input content request to configured_address/moderations.
  2. A completion request to configured_address/chat/completions.
  3. A moderation on content returned by AI to configured_address/moderations.
If your AI service does not require moderation (for example, moderation is already made by chat/completions endpoint) you can disable it by unchecking this checkbox.
* Enable streaming: When this option is disabled, the connector will execute only requests without streaming to AI service. It is useful when the AI service do not support streaming. It is enabled by default. 
* Extra query parameters: Extra name/value parameters to set in the query of the AI requests.
* Extra headers: Extra name/value parameters to set in the headers of the AI requests.

## Positron API overview

The add-on is built on top of the Positron API, which exposes extension points for defining custom AI connectors and services.

### Core concepts

- **`AIConnector`**: Declares a connector that shows up in the Preferences page and wires the UI configuration to an `AIService` implementation.
  - Defines identity and display name (`getConnectorId`, `getConnectorName`).
  - Exposes the list of UI parameters (`getParametersList`) so users can configure the service.
  - Creates the runtime service (`createAIService`) that executes requests.
  - Optionally adapts outgoing requests (`configureCompletionRequest`).

- **`AIService`**: Encapsulates the logic to call the AI backend (chat completions, moderation, streaming, headers/query params, authentication, proxy, timeouts, etc.).

- **Parameters (`ConnectorParamBase`)**: Typed parameters drive both the Preferences UI and, when relevant, the side-view. Common types used by this add-on:
  - `TextFieldConnectorParam` (e.g., base URL, model)
  - `PasswordTextFieldConnectorParam` (e.g., API key)
  - `CheckBoxConnectorParam` (e.g., moderation, streaming)
  - `KeyValueTableConnectorParam` (e.g., extra headers, extra query params)
  - `ModelsComboConnectorParam` (supplies the model combo in Preferences and the side-view)

### How this add-on uses the API

- Declares `CustomAIConnector` (an `AIConnector`) that:
  - Provides the configuration parameters listed above.
  - Registers with Positron via the AIConnectors extension point (see [official guide](https://www.oxygenxml.com/doc/ug-addons/topics/ai_positron_enterprise-extending.html#ai_positron_enterprise-extending__dlentry_zgn_lxf_tfc)). The extension implementation returns a list of connectors; this add-on contributes the “Custom AI Service” (see `plugin.xml`).
  - Constructs a `CustomAIService` using a configuration supplier (base URL, API key or OAuth token, headers, query params, moderation flag).
  - Adjusts outgoing requests (e.g., disables streaming if the user unchecked it, sets a model) in `configureCompletionRequest`.

### Model combo integration (`ModelsComboConnectorParam`)

Use `ModelsComboConnectorParam` to supply a list of available models. Besides showing in Preferences, it also fuels the model selector in the side-view of the add-on.

```java
params.add(new ModelsComboConnectorParam(MODEL_PARAM_ID, "Model:", "Choose the model", new Supplier<List<ModelDescriptor>>() {
  @Override
  public List<ModelDescriptor> get() {
    List<ModelDescriptor> models = new ArrayList<>();
    models.add(new ModelDescriptor("gpt-4.1", "GPT 4.1", "This Gpt 4.1 model"));
    models.add(new ModelDescriptor("gpt-5", "GPT 5",  "This is GPT 5 model"));
    return models;
  }
}).setDefaultValue("gpt-5"));
```

Copyright and License
---------------------
Copyright 2025 Syncro Soft SRL.

This project is licensed under [Apache License 2.0](https://github.com/oxygenxml/oxygen-ai-positron-custom-connector/blob/master/LICENSE)
