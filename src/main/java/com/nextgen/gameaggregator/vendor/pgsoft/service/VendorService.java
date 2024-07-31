package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.GameCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

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

    public static void validatePlayerUsername(String vendorPlayerUsernameFromRequest, String vendorPlayerUsernameFromSession) throws InvalidPlayerException {
        if (!vendorPlayerUsernameFromRequest.equals(vendorPlayerUsernameFromSession)) {
            throw new InvalidPlayerException();
        }
    }

    public static String generateGameUrl(String urlTemplate, String gameCode, String languageCode, String operatorToken, String playerGameSessionToken, String lobbyUrl) {
        // https://m.pg-redirect.net/{gameID}/index.html?l={0}&btt=1&ot={2}&ops={3}&f={4}
        return MessageFormat.format(urlTemplate, gameCode, languageCode, operatorToken, playerGameSessionToken, lobbyUrl);
    }

    public static String generateBetDetailUrl(String urlTemplate, String traceUd, String operatorToken, String parentId, String betId, String languageCode) {
        // https://public.pg-redirect.net/history/redirect.html?trace_id={0}&t={1}&psid={2}&sid={3}&lang={4}&type=operator
        return MessageFormat.format(urlTemplate, traceUd, operatorToken, parentId, betId, languageCode);
    }
}
