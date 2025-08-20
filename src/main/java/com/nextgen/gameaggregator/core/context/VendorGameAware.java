package com.nextgen.gameaggregator.core.context;

public interface VendorGameAware {
    String getGameCode();
    Integer getVendorId();
    void setVendorGameId(Integer vendorGameId);
    void setGameCode(String gameCode);
    void setGameName(String gameName);
    void setGameCategoryId(Integer gameCategoryId);
}
