package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
//import com.nextgen.gameaggregator.operator.game.url.GameUrlDto;
import com.nextgen.gameaggregator.repository.GameSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GameSessionService {
    @Autowired
    private GameSessionRepository gameSessionRepository;

    public GameSession verifyToken(String token) throws AuthenticationException {
        GameSession session = gameSessionRepository.findByToken(token);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);
        return session;
    }

//    public void createSession(GameSession gameSession, GameUrlDto dto, Integer gameId) {
//        gameSession.setLanguage(dto.getLanguage());
//        gameSession.setCurrencyCode(dto.getCurrency());
//        gameSession.setTraceId(dto.getTraceId());
//        gameSession.setVendorGameId(gameId);
//
//        gameSessionRepository.save(gameSession);
//    }
}
