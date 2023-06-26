package com.nextgen.gameaggregator.vendor.evoplay.constant;

public class Formats {
    public static final String VERSION = "1"; // (of API or Callback)
    public static final String SETTINGS_HTTPS = "1"; // 1 = [HTTPS], 0 = [HTTP] (Default HTTP link is returned.) Game URL return in http or https
    public static final String DENOMINATION = "1"; // Denomination of the game.
    // Note: Only several games still support custom denomination. We recommend you to use 1 or default value for this parameter.
    public static final String RETURN_URL_INFO = "1"; // [1] returns link to the game as JSON, [0] redirects to the page with game
    public static final String CALLBACK_VERSION = "2"; // Callbacks protocol version. Default value is 2.

    // callback response
    // no_refund
    public static final Integer NO_RESEND_CALLBACK = 1; // 1 - callback should not be resent
    public static final Integer RESEND_CALLBACK = 0; // 0 - callback can be resent
    // scope
    public static final String SCOPE_INTERNAL = "internal"; // internal - error message is not available to user
    public static final String SCOPE_USER = "user"; // user - error message seen by user
}
