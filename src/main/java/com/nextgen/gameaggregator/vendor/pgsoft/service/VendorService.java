package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.GameCodes;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VendorService extends BaseVendorService {

    private final VendorCurrencyService vendorCurrencyService;

    public VendorService(VendorCurrencyService vendorCurrencyService) {
        this.vendorCurrencyService = vendorCurrencyService;
    }

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

    public static String generateBetDetailUrl(String urlTemplate, String traceUd, String operatorToken, String parentId, String betId, String languageCode) {
        // https://public.pg-redirect.net/history/redirect.html?trace_id={0}&t={1}&psid={2}&sid={3}&lang={4}&type=operator
        return MessageFormat.format(urlTemplate, traceUd, operatorToken, parentId, betId, languageCode);
    }

    public void checkRealTransferAmount(GameSession gameSession, CashTransferInOutDto dto) throws VendorCurrencyNotSupportException, BetFailedException {
        VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndVendorCurrencyCode(gameSession.getVendorId(), gameSession.getVendorCurrencyCode());
        if (dto.getReal_transfer_amount() != null) {
            BigDecimal convertedWinLossAmount = dto.getWinAmount().subtract(dto.getBetAmount()).multiply(vendorCurrency.getFromVendorRate());
            if (convertedWinLossAmount.compareTo(dto.getReal_transfer_amount()) != 0) {
                throw new BetFailedException();
            }
        }
    }

    public boolean isPromoPayout(CashTransferInOutDto dto) {
        // Transaction type:
        // 106: BetPayout
        // 400: BonusToCash
        // 403: FreeGameToCash
        String input = dto.getTransactionId();

        Pattern pattern = Pattern.compile("-403-");
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }
}
