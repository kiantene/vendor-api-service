package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.AuthenticationException;

public interface VendorExceptionMapper<T> {
    String getVendorClassName();
    T onInternalConfigurationError(InternalConfigurationException ex);
    T onAuthenticationError(AuthenticationException ex);
    T onInsufficientBalance(InsufficientBalanceException ex);
    T onInternalError(Throwable ex);
}
