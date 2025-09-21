package com.nextgen.gameaggregator.core.context;

import java.math.BigDecimal;

public interface VendorCurrencyAware {
    Integer getVendorId();

    String getVendorCurrency();

    void setVendorCurrency(String code);

    String getCurrencyCode();

    void setCurrencyCode(String code); // GA's Currency code

    BigDecimal getToVendorRate();

    void setToVendorRate(BigDecimal rate);

    BigDecimal getFromVendorRate();

    void setFromVendorRate(BigDecimal rate);

    Integer getCurrencyId();

    void setCurrencyId(Integer id);
}
