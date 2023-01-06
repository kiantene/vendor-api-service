package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.exception.DisableGameException;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorGameService {

    @Autowired
    private VendorGameRepository vendorGameRepository;

    public void verifyGameStatus(Integer gameId)throws DisableGameException {
        VendorGame vendorGame = vendorGameRepository.findByIdAndStatus(gameId, 1);
        Optional.ofNullable(vendorGame).orElseThrow(DisableGameException::new);
    }
}
