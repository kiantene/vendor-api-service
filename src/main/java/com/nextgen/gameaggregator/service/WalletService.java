package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.repository.GameSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private GameSessionRepository gameSessionRepository;

    public BigDecimal getBalance(String traceId) {
        return new BigDecimal("1000");
    }
}
