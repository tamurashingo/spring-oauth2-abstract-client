## Spring OAuth2 Abstract Client

This library provides a set of abstract classes and interfaces that can be used to implement clients for OAuth2 authentication and authorization with Spring. It is designed to be used primarily in batch processing applications that need to obtain and use access tokens to access resources.

### Features

- Provides a set of abstract classes and interfaces for building OAuth2 clients in Spring.
- Supports OAuth2 authorization code flow.
- Provides a flexible configuration for authentication endpoints and token endpoints.
- (Handles token refresh automatically and transparently.) not implemented.

### Usage

To use this library, you should first create a concrete implementation of the `OAuth2ClientDetails` interface to define the client ID, client secret, and other properties of the OAuth2 client you want to use. Then, you can use the `OAuth2ClientContext` and `OAuth2RestTemplate` classes provided by Spring to obtain and use access tokens.

Here's an example of how to use this library to implement an OAuth2 client for Salesforce:

```java
public class SalesforceOAuth2Client extends AbstractOAuth2Client {

    private static final String AUTHORIZATION_URL = "https://login.salesforce.com/services/oauth2/authorize";
    private static final String TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";

    public SalesforceOAuth2Client(OAuth2ClientDetails clientDetails, OAuth2ClientContext oauth2ClientContext) {
        super(clientDetails, oauth2ClientContext);
    }

    @Override
    protected String getAuthorizationUrl() {
        return AUTHORIZATION_URL;
    }

    @Override
    protected String getTokenUrl() {
        return TOKEN_URL;
    }
}

public class SalesforceService {

    private final OAuth2RestTemplate restTemplate;

    public SalesforceService(OAuth2RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void fetchSalesforceData() {
        ResponseEntity<String> response = restTemplate.getForEntity("https://your.salesforce.com/services/data/v51.0/query?q=SELECT+Name+FROM+Account", String.class);
        // Handle the response...
    }
}
```

### License

This library is licensed under the [MIT License](https://opensource.org/licenses/MIT). See the `LICENSE` file for details.
