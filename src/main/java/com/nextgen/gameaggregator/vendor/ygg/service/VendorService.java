package com.nextgen.gameaggregator.vendor.ygg.service;

import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    public void verifyTokenStatus(Integer status) throws AuthenticationException {
        if (!Objects.equals(status, Status.ACTIVE.code)) {
            throw new AuthenticationException("Token status is not active");
        }
    }

    public void verifyCurrency(String vendorCurrencyCode, String currencyCode) throws IllegalArgumentException {
        if (!vendorCurrencyCode.equals(currencyCode)) {
            throw new IllegalArgumentException("Currency codes do not match");
        }
    }
}
