package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutAction;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.BetTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;

@Service
@Slf4j
public class VendorService {

    public static void validateOperatorTokenAndSecretKey(String operatorTokenFromRequest, String secretKeyFromRequest, String operatorTokenFromDb, String secretKeyFromDb) throws NoAvailableLineException {
        if (!operatorTokenFromRequest.equals(operatorTokenFromDb) || !secretKeyFromRequest.equals(secretKeyFromDb)) {
            throw new NoAvailableLineException();
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

    public static String identifyBetType(CashTransferInOutDto dto) {
        // 1. If parent bet ID = bet ID means this is a bet request
        // if bet amount is 0 and still on going, means free spin
        Boolean isBetRequest = dto.getParentBetId().equals(dto.getBetId());
        Boolean isRoundEnded = dto.getIsEndRound();
        Boolean hasWinAmount = dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0;

        /**
         * Scenario 1
         */
        if (isBetRequest) {
            if (isRoundEnded) {
                if (hasWinAmount) {
                    // Win Bet Request + Result (Together as one)
                    return BetTypes.REQUEST_AND_WIN_AND_END_ROUND;
                } else {
                    // Lose Bet Request + Result (Together as one)
                    return BetTypes.REQUEST_AND_LOSE_AND_END_ROUND;
                }
            } else {
                // Win Bet Request
                return BetTypes.REQUEST_AND_WIN_AND_ONGOING;
            }
        } else { // is not a bet request
            if (isRoundEnded) {
                // Win End Round
                return BetTypes.END_ROUND;
            } else { // round not ended
                if (hasWinAmount) {
                    // Freespin Lose
                    return BetTypes.FREESPIN_WIN_AND_ONGOING;
                } else {
                    // Freespin Win
                    return BetTypes.FREESPIN_LOSE_AND_ONGOING;
                }
            }
        }

//        return BetTypes.UNIDENTIFIABLE;
    }



}
