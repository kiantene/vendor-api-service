package com.nextgen.gameaggregator.core.context;

public interface VendorGameAware {
    String getVendorGameCode();
    Integer getVendorId();
    void setVendorGameId(Integer vendorGameId);
    void setGameCode(String gameCode); // GA's game code
    void setGameName(String gameName);
    void setGameCategoryId(Integer gameCategoryId);

    // Resolved game fields (populated by BaseEnricher#enrichVendorGame from the
    // request's vendorGameCode). Exposed so validators can reconcile the request
    // game onto a session view without re-resolving.
    Integer getVendorGameId();
    String getGameCode();
    Integer getGameCategoryId();
}
