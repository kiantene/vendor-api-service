package com.nextgen.gameaggregator.enums;

public enum Region {

    LATAM("Brazil", "BR"),
    APAC("Philippines", "PH"),
    CIS("Turkey", "TR"),
    NEA("China", "CN"),
    AFRICA("Nigeria", "NG");

    private final String country;
    private final String countryCode;

    Region(String country, String countryCode) {
        this.country = country;
        this.countryCode = countryCode;
    }

    public static String getCountryCodeByRegion(String region) {
        for (Region value : values()) {
            if (value.name().equalsIgnoreCase(region)) {
                return value.getCountryCode();
            }
        }
        return null;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
