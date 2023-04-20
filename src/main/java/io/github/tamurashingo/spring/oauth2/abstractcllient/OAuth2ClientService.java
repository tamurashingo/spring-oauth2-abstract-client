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
package io.github.tamurashingo.spring.oauth2.abstractcllient;

import java.util.Map;

/**
 * OAuth2ClientService class.
 *
 * @author tamura shingo (tamura.shingo@gmail.com)
 */
public interface OAuth2ClientService {

    /**
     * response_type.
     *
     * @return
     */
    String getResponseType();

    /**
     * client_id.
     *
     * @return
     */
    String getClientId();

    /**
     * The client secret.
     * Returns null if not required.
     *
     * @return
     */
    String getClientSecret();

    /**
     * redirect_uri.
     *
     * @return
     */
    String getRedirectUri();

    /**
     * scope.
     * Returns null if not required.
     *
     * @return
     */
    String getScope();

    /**
     * state.
     * Returns null if not required.
     *
     * @return
     */
    String getState();

    /**
     * authorization endpoint uri.
     *
     * @return
     */
    String getAuthorizationUri();

    /**
     * token endpoint uri.
     *
     * @return
     */
    String getTokenUri();

    /**
     * request to the token endpoint by sending the code received from the authorization server.
     * If successful, invokes the registered callback function with the new access token.
     *
     * @param code received from the authorization server
     */
    void fetchAccessToken(String code) throws OAuth2ClientServiceException;

    /**
     * Fetches a new access token using the provided refresh token.
     * If the refresh token is invalid, an OAuth2AuthenticationException will be thrown.
     * If successful, invokes the registered callback function with the new access token.
     *
     * @param refreshToken the refresh token to use to obtain a new access token
     * @throws OAuth2ClientServiceException
     */
    void refreshAccessToken(String refreshToken) throws OAuth2ClientServiceException;

    /**
     * adds callback function.
     * callback is called when authorization server issues tokens.
     *
     * @param callback
     * @see #fetchAccessToken(String)
     * @see #refreshAccessToken(String)
     */
    void addCallback(Callback callback);

    /**
     * removes callback function.
     *
     * @param callback
     */
    void removeCallback(Callback callback);


    /**
     * callback function.
     * callback is called when authorization server issues tokens.
     */
    interface Callback {
        /**
         * authorization server returns the response
         * <pre>{@code
         * {
         *   "access_token":"2YotnFZFEjr1zCsicMWpAA",
         *   "token_type":"example",
         *   "expires_in":3600,
         *   "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
         *   "example_parameter":"example_value"
         * }}</pre>
         * which is set in the result map.
         *
         * @param result
         */
        void callback(Map<String, String> result);
    }


    /**
     * Signals that some error has occurred.
     */
    class OAuth2ClientServiceException extends RuntimeException {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        /**
         * construct a new {@code OAuth2ClientServiceException}
         * with the specified short message.
         *
         * @param message the short message
         */
        public OAuth2ClientServiceException(String message) {
            super(message);
        }

        /**
         * construct a new {@code OAuth2ClientServiceException}
         * with the specified short message and cause.
         *
         * @param message the short message
         * @param cause the cause
         */
        public OAuth2ClientServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
