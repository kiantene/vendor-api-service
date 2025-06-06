package com.nextgen.gameaggregator.entity.ga.custom;

public record IGameVendorDTO(String name, String code, String categoryCode, String currencyCode) {

    public static IGameVendorDTO fromEntity(IGameVendor vendor) {
        return new IGameVendorDTO(
                vendor.getName(),
                vendor.getCode(),
                vendor.getCategoryCode(),
                vendor.getCurrencyCode()
        );
    }
}
