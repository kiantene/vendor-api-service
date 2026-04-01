package com.nextgen.gameaggregator.util;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.InputStream;
import java.net.InetAddress;

public class GeoIpUtil {
    private static DatabaseReader reader;

    static {
        try {
            InputStream dbStream = GeoIpUtil.class
                    .getClassLoader()
                    .getResourceAsStream("GeoLite2-Country.mmdb");

            reader = new DatabaseReader.Builder(dbStream).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCountryCode(String ip) {
        try {
            if (reader == null) {
                return null;
            }
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = reader.country(ipAddress);
            return response.getCountry().getIsoCode(); // MY / US / SG
        } catch (Exception e) {
            return null;
        }
    }
}
