package com.nextgen.gameaggregator.vendor.queenmaker.service;

import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
@Slf4j
@Data
public class VendorService {

    public static String getIpAddress() throws InvalidVendorLineException {
        try (final DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 12345);
            return datagramSocket.getLocalAddress().getHostAddress();
        }catch (Exception e){
            throw new InvalidVendorLineException(e.getMessage());
        }
    }

}
