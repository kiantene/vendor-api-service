package com.nextgen.gameaggregator.core.context;

public interface VendorPlayerAware {
    String getVendorPlayerUsername();

    void setAgentId(Integer agentId);

    void setAgentPlayerId(Long agentPlayerId);

    void setAgentPlayerUsername(String agentPlayerUsername);

    void setVendorPlayerId(Long vendorPlayerId);

    void setVendorId(Integer vendorId);

    void setVendorLineId(Integer vendorLineId);

    void setCurrencyId(Integer currencyId);
}
