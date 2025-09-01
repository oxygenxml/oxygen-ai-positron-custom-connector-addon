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
    * POSITRON_CONNECTOR_AUTH_CLIENT_ID: The ID of the requesting client.
    * POSITRON_CONNECTOR_AUTH_CLIENT_SECRET: The secret of the client.
    * POSITRON_CONNECTOR_AUTH_AUDIENCE: The audience for the token, typically the API or service you're accessing. Optional.
    * POSITRON_CONNECTOR_AUTH_ORGANIZATION: The organization name or identifier. Optional.
    
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

Copyright and License
---------------------
Copyright 2025 Syncro Soft SRL.

This project is licensed under [Apache License 2.0](https://github.com/oxygenxml/oxygen-ai-positron-custom-connector/blob/master/LICENSE)
