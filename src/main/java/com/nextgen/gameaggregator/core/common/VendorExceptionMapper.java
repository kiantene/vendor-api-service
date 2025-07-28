package com.nextgen.gameaggregator.core.common;

public interface VendorExceptionMapper {
    String getVendorClassName();
    Object handle(Throwable ex);
}
