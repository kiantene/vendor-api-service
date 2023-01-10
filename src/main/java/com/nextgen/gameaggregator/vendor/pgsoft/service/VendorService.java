package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
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

    // Overloading with below
    public static void validateVendorGameCode(String vendorGameCodeFromRequest, String vendorGameCodeFromSession) throws GameNotSupportedException {
        if (!vendorGameCodeFromRequest.equals(vendorGameCodeFromSession)) {
            throw new GameNotSupportedException();
        }
    }

    // Overloading with above
    public static void validateVendorGameCode(Integer vendorGameCodeFromRequest, String vendorGameCodeFromSession) throws GameNotSupportedException {
        if (!String.valueOf(vendorGameCodeFromRequest).equals(vendorGameCodeFromSession)) {
            throw new GameNotSupportedException();
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


    public static Boolean isBetRequest(CashTransferInOutDto dto) {
        return dto.getParentBetId().equals(dto.getBetId());
    }

    public static Boolean isRoundEnded(CashTransferInOutDto dto) {
        return dto.getIsEndRound();
    }

    public static Boolean hasWinAmount(CashTransferInOutDto dto) {
        return dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public static Boolean isResentForValidate(CashTransferInOutDto dto) {
        return dto.getIsValidateBet() != null && dto.getIsValidateBet() == true;
    }

    public static String identifyBetType(CashTransferInOutDto dto) {
        // To determine a bet request, parent bet ID must be equalise to bet ID
        Boolean isBetRequest = dto.getParentBetId().equals(dto.getBetId());
        Boolean isRoundEnded = dto.getIsEndRound();
        // if bet amount is 0 and still on going, means free spin
        Boolean hasWinAmount = dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0;
        Boolean isResentForValidate = dto.getIsValidateBet() != null && dto.getIsValidateBet() == true;

        /**
         * Scenario 1
         */
        if (isResentForValidate) {
            return BetTypes.RESENT_FOR_VALIDATION;
        }
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
                if (hasWinAmount) {
                    // Win Bet Request
                    return BetTypes.REQUEST_AND_WIN_AND_ONGOING;
                } else {
                    // Lose Bet Request
                    return BetTypes.REQUEST_AND_LOSE_AND_ONGOING;
                }
            }
        } else { // is not a bet request
            if (isRoundEnded) {
                if (hasWinAmount) {
                    // Freespin Lose
                    return BetTypes.WIN_AND_END_ROUND;
                } else {
                    // Freespin Win
                    return BetTypes.LOSE_AND_END_ROUND;
                }
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
