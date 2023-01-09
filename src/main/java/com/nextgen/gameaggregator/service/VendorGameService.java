package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorGameService {

    @Autowired
    private VendorGameRepository vendorGameRepository;

    public void verifyGameStatus(Integer gameId) throws DisabledGameException {
        VendorGame vendorGame = vendorGameRepository.findByIdAndStatus(gameId, Status.ACTIVE.code);
        Optional.ofNullable(vendorGame).orElseThrow(DisabledGameException::new);
    }
}
