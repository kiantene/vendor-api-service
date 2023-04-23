package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.GameCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VendorService {

    public static void validateOperatorTokenAndSecretKey(String operatorTokenFromRequest, String secretKeyFromRequest, String operatorTokenFromDb, String secretKeyFromDb) throws NoAvailableLineException {
        if (!operatorTokenFromRequest.equals(operatorTokenFromDb) || !secretKeyFromRequest.equals(secretKeyFromDb)) {
            throw new NoAvailableLineException();
        }
    }

    public static void validateVendorGameCode(String vendorGameCodeFromRequest, String vendorGameCodeFromSession) throws GameNotSupportedException {
        // Only proceed to validate if this game session not open via PGS Game Lobby
        if (!vendorGameCodeFromSession.equals(GameCodes.LOBBY_CODE)) {
            if (!vendorGameCodeFromRequest.equals(vendorGameCodeFromSession)) {
                throw new GameNotSupportedException();
            }
        }
    }

    public static void validateGameStatus(VendorGame game) throws GameNotSupportedException {
        if (game.getStatus() == 0) {
            throw new GameNotSupportedException();
        }
    }

    public static void validateVendorCurrencyCode(String vendorCurrencyCodeFromRequest, String vendorCurrencyCodeFromSession) throws CurrencyNotSupportedException{
        if (!vendorCurrencyCodeFromRequest.equals(vendorCurrencyCodeFromSession)) {
            throw new CurrencyNotSupportedException();
        }
    }

    public static void validatePlayerUsername(String vendorPlayerUsernameFromRequest, String vendorPlayerUsernameFromSession) throws InvalidPlayerException {
        if (!vendorPlayerUsernameFromRequest.equals(vendorPlayerUsernameFromSession)) {
            throw new InvalidPlayerException();
        }
    }

    public static String generateGameUrl(String urlTemplate, String gameCode, String languageCode, String operatorToken, String playerGameSessionToken) {
        // https://m.pg-redirect.net/{gameID}/index.html?l={0}&btt=1&ot={2}&ops={3}
        String gameUrl = MessageFormat.format(urlTemplate, gameCode, languageCode, operatorToken, playerGameSessionToken);
        return gameUrl;
    }

    public static String generateBetDetailUrl(String urlTemplate, String traceUd, String operatorToken, String parentId, String betId, String languageCode) {
        // https://public.pg-redirect.net/history/redirect.html?trace_id={0}&t={1}&psid={2}&sid={3}&lang={4}&type=operator
        String betDetailUrl = MessageFormat.format(urlTemplate, traceUd, operatorToken, parentId, betId, languageCode);
        return betDetailUrl;
    }

    public static String generateBetDetailUrl(String apiUrl, MultiValueMap<String, String> parameters) {
        // form query string
        String queryString = "";
        List<String> values = new ArrayList<>();
        for (String key : parameters.keySet()){
            values.add(key + "=" + parameters.getFirst(key));
        }

        String betDetailUrl = apiUrl + "?" + String.join("&", values);

        return betDetailUrl;
    }


    public static Boolean isBetRequest(CashTransferInOutDto dto) {
        return dto.getParentBetId().equals(dto.getVendorBetId());
    }

//    public static Boolean isRoundEnded(CashTransferInOutDto dto) {
//        return dto.getIsEndRound();
//    }

    public static Boolean hasWinAmount(CashTransferInOutDto dto) {
        return dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public static Boolean isResentForValidate(CashTransferInOutDto dto) {
        return dto.getIsValidateBet() != null && dto.getIsValidateBet() == true;
    }

    public static Boolean isFeatureBuy(CashTransferInOutDto dto) {
        return dto.getIsFeatureBuy() != null && dto.getIsFeatureBuy() == true;
    }


}
