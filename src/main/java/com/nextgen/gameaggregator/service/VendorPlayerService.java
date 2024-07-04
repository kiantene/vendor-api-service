package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class VendorPlayerService {
    private final VendorPlayerRepository vendorPlayerRepository;
    private final GameSessionService gameSessionService;

    @Autowired
    public VendorPlayerService(VendorPlayerRepository vendorPlayerRepository, GameSessionService gameSessionService) {
        this.vendorPlayerRepository = vendorPlayerRepository;
        this.gameSessionService = gameSessionService;
    }

    @Cacheable(value = "VendorPlayerUsername", key = "#username", cacheManager = "cacheManager")
    public VendorPlayer getVendorPlayerByUsername(String username) throws InvalidPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(username);

        return Optional.ofNullable(vendorPlayer).orElseThrow(InvalidPlayerException::new);
    }

    @Transactional
    public VendorPlayer updateNewVendorPlayerUsername(GameSession gameSession, String newVendorPlayerUsername) throws InvalidPlayerException {

        VendorPlayer vendorPlayer = getVendorPlayerByUsername(gameSession.getVendorPlayerUsername());
        vendorPlayer.setUsername(newVendorPlayerUsername);

        gameSessionService.clearGameSession(gameSession, gameSession.getAgentPlayerUsername(), gameSession.getVendorGameCode());
        gameSession.setVendorPlayerUsername(newVendorPlayerUsername);
        gameSession.setStatus(Status.ACTIVE.code);
        gameSessionService.updateSession(gameSession);

        return vendorPlayerRepository.saveAndFlush(vendorPlayer);
    }


    @Cacheable(value = "VendorPlayers", key = "#vendorPlayerId", cacheManager = "cacheManager")
    public VendorPlayer getByVendorPlayerId(Long vendorPlayerId, VendorPlayer vendorPlayer) throws InvalidPlayerException {
        if (vendorPlayer == null) {
            vendorPlayer = vendorPlayerRepository.findById(vendorPlayerId).orElse(null);
            Optional.ofNullable(vendorPlayer).orElseThrow(InvalidPlayerException::new);
        }
        return vendorPlayer;
    }

}
