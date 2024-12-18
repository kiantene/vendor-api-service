package com.nextgen.gameaggregator.vendor.evolutionlive.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.BetNotFoundLog;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.BetNotFoundLogService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.evolutionlive.api.endround.CreditDto;
import com.nextgen.gameaggregator.vendor.evolutionlive.api.gameurl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private BetNotFoundLogService betNotFoundLogService;
    @Autowired
    private GameSessionService gameSessionService;

    public static Long getTimestamp() {
        return Instant.now().toEpochMilli();
    }

    public PlayerSessionDto setPlayerSessionDto(GameSession gameSession) {
        PlayerSessionDto playerSessionDto = new PlayerSessionDto();
        playerSessionDto.setId(gameSession.getVendorToken());
        playerSessionDto.setIp(gameSession.getIpAddress());
        return playerSessionDto;
    }

    public PlayerDto setPlayerDto(GameSession gameSession, PlayerSessionDto playerSessionDto, String countryCode) {
        PlayerDto playerDto = new PlayerDto();
        playerDto.setId(gameSession.getVendorPlayerUsername());
        playerDto.setUpdate(true);
        playerDto.setFirstName(gameSession.getVendorPlayerUsername());
        playerDto.setLastName(gameSession.getVendorPlayerUsername());
        playerDto.setCountry(countryCode);
        playerDto.setLanguage(gameSession.getVendorLanguageCode());
        playerDto.setCurrency(gameSession.getVendorCurrencyCode());
        playerDto.setSession(playerSessionDto);
        return playerDto;
    }

    public ConfigChannelDto setConfigChannelDto(GameSession gameSession) {
        ConfigChannelDto configChannelDto = new ConfigChannelDto();
        configChannelDto.setWrapped(false);
        configChannelDto.setMobile(Boolean.valueOf(gameSession.getVendorPlatformCode()));
        return configChannelDto;
    }

    public ConfigUrlsDto setConfigUrlsDto(GameSession gameSession) {
        ConfigUrlsDto configUrlsDto = new ConfigUrlsDto();
        configUrlsDto.setLobby(gameSession.getLobbyUrl());
        return configUrlsDto;
    }

    public GameTableDto setGameTableDto(GameSession gameSession) {
        GameTableDto gameTableDto = new GameTableDto();
        gameTableDto.setId(gameSession.getVendorGameCode());
        return gameTableDto;
    }

    public ConfigGameDto setConfigGameDto(GameTableDto gameTableDto, String categoryCode) {
        ConfigGameDto configGameDto = new ConfigGameDto();
        if (categoryCode != null && !categoryCode.isBlank()) {
            // into game category lobby
            configGameDto.setCategory(categoryCode);
        } else {
            // into direct game
            configGameDto.setTable(gameTableDto);
        }
        return configGameDto;
    }

    public ConfigDto setConfigDto(ConfigGameDto configGameDto, ConfigChannelDto configChannelDto, ConfigUrlsDto configUrlsDto) {
        ConfigDto configDto = new ConfigDto();
        configDto.setGame(configGameDto);
        configDto.setChannel(configChannelDto);
        configDto.setUrls(configUrlsDto);
        return configDto;
    }

    public void verifyDebitAfterRollback(Long vendorPlayerId, String externalTransactionId) throws DuplicateExternalTransactionIdException {
        BetNotFoundLog betNotFoundLog = betNotFoundLogService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        // if have data mean have call rollback before
        if (betNotFoundLog != null) {
            throw new DuplicateExternalTransactionIdException();
        }
    }

    @Override
    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {
        // Get the JSON request body from the HttpRequestLog
        String requestBody = rawData;
        Gson gson = new Gson();

        try {
            // Convert the JSON request body to SettleDto object
            CreditDto dto = gson.fromJson(requestBody, CreditDto.class);

            // Remap roundId before 20th Dec 2024
            if (settledBet.getVendorBetTime() < 1734652800000L) {
                settledBet.setRoundId(dto.getGame().getId().split("-")[0]);
            } else {
                settledBet.setRoundId(dto.getGame().getId());
            }


        } catch (JsonParseException e) {
            log.error("Error parsing JSON: " + e.getMessage());
        }

        return settledBet;
    }

    public GameSession preCheckGameSessionToken(String token) throws AuthenticationException {
        GameSession gameSession = null;
        try {
            gameSession = gameSessionService.verifyVendorToken(token);
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.verifyToken(token);
        }

        if (Objects.isNull(gameSession.getVendorToken())) {
            gameSession.setVendorToken(gameSession.getToken());
            gameSessionService.updateSession(gameSession);
        }

        return gameSession;
    }
}
