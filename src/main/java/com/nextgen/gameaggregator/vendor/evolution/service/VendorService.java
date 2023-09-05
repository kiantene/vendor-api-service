package com.nextgen.gameaggregator.vendor.evolution.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.entity.BetNotFoundLog;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.BetNotFoundLogService;
import com.nextgen.gameaggregator.vendor.evolution.api.endround.CreditDto;
import com.nextgen.gameaggregator.vendor.evolution.api.gameurl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private BetNotFoundLogService betNotFoundLogService;

    public static Long getTimestamp() {
        return Instant.now().toEpochMilli();
    }

    public PlayerSessionDto setPlayerSessionDto(GameSession gameSession) {
        PlayerSessionDto playerSessionDto = new PlayerSessionDto();
        playerSessionDto.setId(gameSession.getToken());
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

    public GameTableDto setGameTableDto(GameSession gameSession) {
        GameTableDto gameTableDto = new GameTableDto();
        gameTableDto.setId(gameSession.getVendorGameCode());
        return gameTableDto;
    }

    public ConfigGameDto setConfigGameDto(GameTableDto gameTableDto) {
        ConfigGameDto configGameDto = new ConfigGameDto();
        configGameDto.setTable(gameTableDto);
        return configGameDto;
    }

    public ConfigDto setConfigDto(ConfigGameDto configGameDto, ConfigChannelDto configChannelDto) {
        ConfigDto configDto = new ConfigDto();
        configDto.setGame(configGameDto);
        configDto.setChannel(configChannelDto);
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

            // Remap roundId
            settledBet.setRoundId(dto.getGame().getId());


        } catch (JsonParseException e) {
            log.error("Error parsing JSON: " + e.getMessage());
        }

        return settledBet;
    }
}
