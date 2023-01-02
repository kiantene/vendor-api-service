package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorPlayerService {
    @Autowired
    private VendorPlayerRepository vendorPlayerRepository;

    public VendorPlayer getVendorPlayerByUsername(String username) throws InvalidPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(username);
        Optional.ofNullable(vendorPlayer).orElseThrow(InvalidPlayerException::new);

        return vendorPlayer;
    }
}
