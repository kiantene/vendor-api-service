package com.nextgen.gameaggregator.vendor.facai.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    SettledBetService settledBetService;
    private static final String GET_PUBLIC_IP = "https://checkip.amazonaws.com";

    public boolean isValidDateString(String timestamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try {
            Date date = dateFormat.parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Integer operatorTimeoutTiming() {
        //fc vendor timeout is 4000, and given 500 buffer timing, then defaultTiming would be 3500.
        Integer operatorDefaultTiming = 3500;
        return operatorDefaultTiming;
    }

    public boolean isNotPublicIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);

            // Check common non-public address types
            if (address.isAnyLocalAddress()      // 0.0.0.0
                    || address.isLoopbackAddress()  // 127.0.0.1
                    || address.isLinkLocalAddress() // 169.254.x.x
                    || address.isSiteLocalAddress() // RFC1918 private ranges
            ) {
                return true;
            }

            // Manually check CGNAT range (100.64.0.0/10)
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }

            return false;

        } catch (Exception e) {
            // Treat invalid or unresolvable IP as non-public
            return true;
        }
    }

    public String getPublicIp() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new URL(GET_PUBLIC_IP).openStream()))) {
            return reader.readLine();
        } catch (Exception e) {
            return null;
        }
    }
}
