/*-
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 tamura shingo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.tamurashingo.spring.oauth2.abstractcllient.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tamurashingo.spring.oauth2.abstractcllient.OAuth2ClientService;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract implementation of the OAuth2ClientService interface that provides common functionality for
 * retrieving tokens using the authorization code grant type.
 */
public abstract class AbstractOAuth2ClientServiceImpl implements OAuth2ClientService {

    /**
     * Map that stores the names of the parameters used in authentication requests and their corresponding
     * getter methods in the OAuth2ClientService interface.
     */
    Map<String, Method> authenticationParams;

    /**
     * Map that stores the names of the parameters used in token requests and their corresponding
     * getter methods in the OAuth2ClientService interface.
     */
    Map<String, Method> tokenParams;

    /**
     * Map that stores the names of the parameters used in refresh token requests and their corresponding
     * getter methods in the OAuth2ClientService interface.
     */
    Map<String, Method> refreshTokenParams;

    /**
     * List of callbacks that will be executed when a token is retrieved.
     */
    protected List<Callback> callbacks = new LinkedList<>();

    /**
     * constructor
     *
     * @throws OAuth2ClientServiceException
     */
    public AbstractOAuth2ClientServiceImpl() throws OAuth2ClientServiceException {
        authenticationParams = initializeAuthenticationParams();
        tokenParams = initializeTokenParams();
    }

    public String generateAuthenticationUri() {
        MultiValueMap<String, String> params = generateParameters(authenticationParams, true);

        return UriComponentsBuilder.fromUriString(getAuthorizationUri())
                .queryParams(params)
                .build()
                .toUriString()
                ;
    }

    @Override
    public void addCallback(Callback callback) {
        this.callbacks.add(callback);
    }

    @Override
    public void removeCallback(Callback callback) {
        this.callbacks.remove(callback);
    }

    /**
     * Initializes the authentication parameters map with the names of the parameters used in authentication requests
     * and their corresponding getter methods in the OAuth2ClientService interface.
     *
     * @return The initialized authentication parameters map.
     * @throws OAuth2ClientServiceException If there is an error initializing the map.
     */
    Map<String, Method> initializeAuthenticationParams() throws OAuth2ClientServiceException {
        try {
            Map<String, Method> params = new HashMap<>();
            params.put("response_type", OAuth2ClientService.class.getMethod("getResponseType"));
            params.put("client_id", OAuth2ClientService.class.getMethod("getClientId"));
            params.put("redirect_uri", OAuth2ClientService.class.getMethod("getRedirectUri"));
            params.put("scope", OAuth2ClientService.class.getMethod("getScope"));
            params.put("state", OAuth2ClientService.class.getMethod("getState"));
            return params;
        } catch (NoSuchMethodException ex) {
            throw new OAuth2ClientServiceException("initialize error", ex);
        }
    }


    /**
     * Initializes the token parameters map with the names of the parameters used in token requests
     * and their corresponding getter methods in the OAuth2ClientService interface.
     *
     * @return The initialized token parameters map.
     * @throws OAuth2ClientServiceException If there is an error initializing the map.
     */
    Map<String, Method> initializeTokenParams() throws OAuth2ClientServiceException {
        try {
            Map<String, Method> params = new HashMap<>();
            params.put("redirect_uri", OAuth2ClientService.class.getMethod("getRedirectUri"));
            params.put("client_id", OAuth2ClientService.class.getMethod("getClientId"));
            params.put("client_secret", OAuth2ClientService.class.getMethod("getClientSecret"));
            return params;
        } catch (NoSuchMethodException ex) {
            throw new OAuth2ClientServiceException("initialize error", ex);
        }
    }

    /**
     * Initializes the refresh token parameters map with the names of the parameters used in refresh token requests
     * and their corresponding getter methods in the OAuth2ClientService interface.
     *
     * @return The initialized refresh token parameters map.
     * @throws OAuth2ClientServiceException If there is an error initializing the map.
     */
    Map<String, Method> initializeRefreshTokenParams() throws OAuth2ClientServiceException {
        try {
            Map<String, Method> params = new HashMap<>();
            params.put("refresh_token", OAuth2ClientService.class.getMethod("getRefreshToken"));

            return params;
        } catch (NoSuchMethodException ex) {
            throw new OAuth2ClientServiceException("initialize error", ex);
        }
    }

    @Override
    public String getResponseType() {
        return "code";
    }

    @Override
    public void fetchAccessToken(String code) throws OAuth2ClientServiceException {
        String response = exchangeWithCode(code);
        Map<String, String> result = parseResponse(response);

        callback(result);
    }

    @Override
    public void refreshAccessToken(String refreshToken) throws OAuth2ClientServiceException {
        String response = exchangeWithRefreshToken(refreshToken);
        Map<String, String> result = parseResponse(response);

        callback(result);
    }

    /**
     * Executes a token exchange using the given authorization code.
     *
     * @param code the authorization code.
     * @return the response.
     */
    protected String exchangeWithCode(String code) {
        MultiValueMap params = generateParameters(tokenParams);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        WebClient webClient = WebClient.builder()
                .baseUrl(getTokenUri())
                .defaultHeaders(this::addDefaultHeaders)
                .build();
        return webClient.post()
                .bodyValue(params)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(
                        new OAuth2ClientServiceException("response error:" + clientResponse.rawStatusCode())))
                .bodyToMono(String.class)
                .block()
                ;
    }

    protected void addDefaultHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    /**
     * Executes a token exchange using the given refresh token
     *
     * @param refreshToken refresh_token.
     * @return the response.
     */
    protected String exchangeWithRefreshToken(String refreshToken) {
        MultiValueMap params = generateParameters(refreshTokenParams);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        WebClient webClient = WebClient.builder()
                .baseUrl(getTokenUri())
                .defaultHeaders(this::addDefaultHeaders)
                .build();
        return webClient.post()
                .bodyValue(params)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(
                        new OAuth2ClientServiceException("response error:" + clientResponse.rawStatusCode())))
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Parses the response JSON string into a map of strings.
     *
     * @param json the response JSON string to be parsed
     * @return a map of key-value pairs representing the parsed response
     * @throws OAuth2ClientServiceException if the response string cannot be parsed
     */
    protected Map<String, String> parseResponse(String json) throws OAuth2ClientServiceException {
        ObjectMapper om = new ObjectMapper();
        TypeReference<HashMap<String, String>> reference = new TypeReference<HashMap<String, String>>() {};
        try {
            return om.readValue(json, reference);
        } catch (JsonProcessingException ex) {
            throw new OAuth2ClientServiceException("response parse error", ex);
        }
    }

    /**
     * Calls all the registered callbacks with the given result.
     * @param result the result to pass to the callbacks
     */
    protected void callback(Map<String, String> result) {
        callbacks.stream().forEach(f -> f.callback(result));
    }


    /**
     * Generates a {@link MultiValueMap} of parameters for a request using the given map of parameter methods.
     * @param parameterMethods a map of parameter names and corresponding getter methods
     * @return a MultiValueMap of parameters for a request
     * @throws OAuth2ClientServiceException if there is an error retrieving parameter values using the getter methods
     */
    protected MultiValueMap<String, String> generateParameters(Map<String, Method> parameterMethods) throws OAuth2ClientServiceException {
        return new LinkedMultiValueMap<String, String>(parameterMethods.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> Arrays.asList(invokeGetter(e.getValue()), "UTF-8"))));
    }

    protected MultiValueMap<String, String> generateParameters(Map<String, Method> parameterMethods, boolean encode) throws OAuth2ClientServiceException {
        final Function<String, String> encoder = encode ? s -> this.encode(s) : Function.identity();

        return new LinkedMultiValueMap<String, String>(parameterMethods.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> Arrays.asList(encoder.apply(invokeGetter(e.getValue()))))));
    }

    /**
     * Invoke a getter method and return the result as a String.
     * @param method the getter method to be invoked
     * @return the result of the invocation as a String
     * @throws RuntimeException if the invocation fails for any reason
     */
    protected String invokeGetter(Method method) throws RuntimeException {
        try {
            return (String)method.invoke(this);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String encode(String str) throws RuntimeException {
        try {
            if (str == null) {
                return null;
            } else {
                return URLEncoder.encode(str, "UTF-8")
                        .replaceAll("\\+", "%20")
                        .replaceAll("\\%21", "!")
                        .replaceAll("\\%27", "'")
                        .replaceAll("\\%28", "(")
                        .replaceAll("\\%29", ")")
                        .replaceAll("\\%7E", "~");
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
