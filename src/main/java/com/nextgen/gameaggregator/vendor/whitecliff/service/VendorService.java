package com.nextgen.gameaggregator.vendor.whitecliff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.whitecliff.api.bet.DebitDto;
import com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl.PrdDto;
import com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.BigInteger;



@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private SettledBetService settledBetService;

    private GameSessionService gameSessionService;

    @Autowired
    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static UserDto setUserDto(GameSession gameSession) {
        UserDto userDto = new UserDto();
        userDto.setName(gameSession.getVendorPlayerUsername());
        userDto.setId(BigInteger.valueOf(gameSession.getVendorPlayerId()));
        userDto.setBalance(BigDecimal.valueOf(0.00));
        userDto.setLanguage(gameSession.getVendorLanguageCode());
        userDto.setCurrency(gameSession.getVendorCurrencyCode());
        userDto.setSid(gameSession.getToken());
        return userDto;
    }

    public static PrdDto setPrdDto(GameSession gameSession, String productId) {
        PrdDto prdDto = new PrdDto();
        prdDto.setId(Integer.valueOf(productId));

        if (gameSession.getGameCategoryId() == 5){
            prdDto.setTable_id(gameSession.getVendorGameCode());
        }

        else {
            prdDto.setType(Integer.valueOf(gameSession.getVendorGameCode()));
        }

        prdDto.setIs_mobile(gameSession.getVendorPlatformCode().equals("H5"));
        return prdDto;
    }

    public static String convertMapToJson(MultiValueMap<String, String> dataMap){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(dataMap.toSingleValueMap());
        } catch (Exception e) {
            return null;
        }
    }

}
