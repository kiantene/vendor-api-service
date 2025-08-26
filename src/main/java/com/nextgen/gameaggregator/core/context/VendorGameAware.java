package com.nextgen.gameaggregator.core.context;

public interface VendorGameAware {
    String getVendorGameCode();
    Integer getVendorId();
    void setVendorGameId(Integer vendorGameId);
    void setGameCode(String gameCode); // GA's game code
    void setGameName(String gameName);
    void setGameCategoryId(Integer gameCategoryId);
}
