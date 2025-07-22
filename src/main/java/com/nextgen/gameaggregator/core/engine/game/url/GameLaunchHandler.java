package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.core.webclient.WebClientHandler;

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
 *       Encryption is handled inside {@link #buildRequestBody(GameLaunchContext)},
 *       and optional headers or metadata may be added via {@link #getHeaders(GameLaunchContext, Object)}.</li>
 *
 *   <li><b>5. No external request — internal HTML generation:</b><br>
 *       No call is made to any external API. Instead, an HTML page is generated internally
 *       using template logic. {@link #buildRequestBody(GameLaunchContext)} is still used
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
 * @param <Q> the request body type to be sent to the vendor
 * @param <R> the response body type expected from the vendor
 */
public interface GameLaunchHandler<Q, R> extends WebClientHandler<Q, R, GameLaunchContext> {
    String NAME = "GameLauncher";
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
    void onSuccess(GameLaunchContext context, R response);
}
