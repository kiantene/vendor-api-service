package com.nextgen.gameaggregator.core.engine.game.url;

public enum GameLaunchMode {
    API_CALL,              // JSON/FORM request + JSON response
    HTML_RESPONSE,         // Any request + HTML response to be saved
    STATIC_HTML,           // No request, HTML generated internally
    ENCRYPTED_API_CALL,     // Request must be encrypted before sending
    QUERY_STRING_URL
}
