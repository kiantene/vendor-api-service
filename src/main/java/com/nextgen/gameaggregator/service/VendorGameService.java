package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorGameService {

    private final VendorGameRepository vendorGameRepository;

    @Autowired
    public VendorGameService(VendorGameRepository vendorGameRepository) {
        this.vendorGameRepository = vendorGameRepository;
    }

    @Cacheable(value = "VendorGames", key = "#gameId", cacheManager = "cacheManager")
    public VendorGame verifyGameStatus(Integer gameId) throws DisabledGameException {
        VendorGame vendorGame = vendorGameRepository.findById(gameId).orElse(null);
        Optional.ofNullable(vendorGame).orElseThrow(DisabledGameException::new);
        if (!vendorGame.getStatus().equals(Status.ACTIVE.code)) {
            throw new DisabledGameException();
        }
        return vendorGame;
    }

    @Cacheable(value = "VendorGames", key = "{#vendorGameCode, #vendorId}", cacheManager = "cacheManager")
    public VendorGame getByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) throws GameNotSupportedException {

        VendorGame vendorGame = vendorGameRepository.findByVendorGameCodeAndVendorId(vendorGameCode, vendorId);
        vendorGame = Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
        if (vendorGame.getStatus() == 0) {
            throw new GameNotSupportedException();
        }
        return vendorGame;
    }

    @Cacheable(value = "VendorGames", key = "{#vendorGameCode, #vendorId}", cacheManager = "cacheManager")
    public VendorGame getByVendorIdAndVendorGameCode(Integer vendorId, String vendorGameCode) throws GameNotSupportedException {

        VendorGame vendorGame = vendorGameRepository.findByVendorGameCodeAndVendorId(vendorGameCode, vendorId);
        return Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
    }

    @Cacheable(value = "VendorGames", key = "{#vendorGameId}", cacheManager = "cacheManager")
    public VendorGame getByVendorGameId(Integer vendorGameId) throws GameNotSupportedException {
        VendorGame vendorGame = vendorGameRepository.findById(vendorGameId).orElseThrow(GameNotSupportedException::new);
        if (vendorGame.getStatus() == 0) {
            throw new GameNotSupportedException();
        }
        return vendorGame;
    }

    @Cacheable(value = "VendorGames", key = "{#gameCode}", cacheManager = "cacheManager")
    public VendorGame checkGameSupported(String gameCode) throws GameNotSupportedException, DisabledGameException {
        VendorGame vendorGame = vendorGameRepository.findByCode(gameCode);
        Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
        if (vendorGame.getStatus() == 0) {
            throw new DisabledGameException();
        }

        return vendorGame;
    }

    @Cacheable(value = "VendorGames", key = "#root.methodName + '_' + #gameId", cacheManager = "cacheManager")
    public VendorGame getByGameId(Integer gameId, VendorGame vendorGame) throws GameNotSupportedException {
        if (vendorGame == null) {
            vendorGame = vendorGameRepository.findById(gameId).orElse(null);
            Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
        }

        return vendorGame;
    }

}
