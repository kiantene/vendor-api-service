package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.Map;

/**
 * A generic interface for handling vendor-specific game launch workflows.
 *
 * <p>This interface defines the contract for integrating with third-party game vendors
 * by abstracting request construction, endpoint targeting, content formatting,
 * and response handling into a consistent, type-safe structure.
 *
 * <p><strong>Supported scenarios:</strong></p>
 * <ul>
 *   <li><b>1. JSON request → JSON response:</b><br>
 *       The most common pattern. A JSON-formatted request is sent to the vendor,
 *       and a JSON response is returned (typically containing a launch URL or metadata).</li>
 *
 *   <li><b>2. Form-urlencoded request → JSON response:</b><br>
 *       Some vendors expect {@code application/x-www-form-urlencoded} requests,
 *       but still return a JSON payload in the response.</li>
 *
 *   <li><b>3. Any request type → raw HTML response:</b><br>
 *       Vendors may respond with a full HTML document (e.g., an iframe or redirect page).
 *       The HTML is captured, uploaded to S3 (or another storage), and a link to the stored
 *       file is returned as the final result.</li>
 *
 *   <li><b>4. Encrypted request → response:</b><br>
 *       Some vendors require the request payload to be encrypted (e.g., AES or RSA).
 *       Encryption is handled inside {@link #onPrepareRequestBody(GameLaunchContext)},
 *       and optional headers or metadata may be added via {@link #getHeaders(GameLaunchContext, Object)}.</li>
 *
 *   <li><b>5. No external request — internal HTML generation:</b><br>
 *       No call is made to any external API. Instead, an HTML page is generated internally
 *       using template logic. {@link #onPrepareRequestBody(GameLaunchContext)} is still used
 *       to prepare dynamic values that populate placeholders in the template. The generated
 *       HTML is saved to S3, and the resulting link is returned.</li>
 * </ul>
 *
 * <p>Each implementation is responsible for:</p>
 * <ul>
 *   <li>Defining the vendor identity via {@link #getVendorClassName()}</li>
 *   <li>Specifying the endpoint and HTTP configuration</li>
 *   <li>Constructing the request payload from {@link GameLaunchContext}</li>
 *   <li>Providing the expected response type for parsing</li>
 *   <li>Handling success or failure via callbacks</li>
 * </ul>
 *
 * @param <R> the request body type to be sent to the vendor
 * @param <T> the response body type expected from the vendor
 */
public interface GameLaunchHandler<R, T> {
    /**
     * Indicates the mode of this handler — used to route control flow in the launcher.
     * Defaults to {@link GameLaunchMode#API_CALL}.
     *
     * @return the scenario type for this vendor
     */
    default GameLaunchMode getLaunchMode() {
        return GameLaunchMode.API_CALL;
    }

    /**
     * Returns the identifier or class name of the vendor.
     * This is used for internal mapping and logging purposes.
     *
     * @return the vendor's identifier or class name
     */
    String getVendorClassName();

    /**
     * Returns the content type of the request body.
     * Default is {@code application/x-www-form-urlencoded}.
     *
     * @return the {@link MediaType} used for the request
     */
    default MediaType getContentType() {
        return MediaType.APPLICATION_FORM_URLENCODED;
    }

    /**
     * Returns the base URL for the vendor's game launch endpoint.
     *
     * @param context the current launch context
     * @return the base URL as a string
     */
    String getBaseUrl(GameLaunchContext context);

    /**
     * Returns the endpoint path to be appended to the base URL.
     *
     * @return the endpoint path (e.g. "/launch")
     */
    String getPath();

    /**
     * Returns the {@link ParameterizedTypeReference} used to deserialize the response.
     *
     * @return the response type reference
     */
    ParameterizedTypeReference<T> getResponseType();

    /**
     * Prepares the request body based on the given launch context.
     *
     * @param context the current launch context
     * @return the request body object
     */
    R onPrepareRequestBody(GameLaunchContext context);

    /**
     * Returns custom headers to be sent with the request.
     * Defaults to an empty map.
     *
     * @param context the current launch context
     * @param requestObject the prepared request body
     * @return a map of headers
     */
    default Map<String, String> getHeaders(GameLaunchContext context, R requestObject) {
        return Collections.emptyMap();
    }

    /**
     * Optional method to provide a raw HTML template for handlers that generate static HTML.
     * Used in {@link GameLaunchMode#STATIC_HTML} scenario.
     *
     * @return the HTML template as a string, or {@code null} if not applicable
     */
    default String getHtmlTemplate() {
        return "";
    }

    /**
     * Callback invoked when the game launch response is successfully received and parsed.
     *
     * @param context the current launch context
     * @param response the deserialized response object
     */
    void onSuccess(GameLaunchContext context, T response);

    /**
     * Callback invoked when an error occurs during the launch process.
     * Default implementation is a no-op.
     *
     * @param context the current launch context
     * @param error the exception or error thrown
     */
    default void onError(GameLaunchContext context, Throwable error) {
        // Optional override
    }
}
