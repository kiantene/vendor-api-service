package com.nextgen.gameaggregator.vendor.ezugi.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.ezugi.api.rollback.RollbackDto;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private VendorGameCodeService vendorGameCodeService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private BetNotFoundLogService betNotFoundLogService;
    @Autowired
    private GameSessionService gameSessionService;

    public static void verifyHash(String secretKey, String data, String hashKey) throws InvalidKeyException, NoSuchAlgorithmException, InvalidSignatureException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(),
                "HmacSHA256");
        sha256_HMAC.init(secret_key);
        String generatedHash = Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes()));
        if (hashKey == null || !hashKey.equals(generatedHash)) {
            String msg = "Expected hash: " + generatedHash + ", but received: " + hashKey;
            log.error("Request body: " + data);
            log.error(msg);
            throw new InvalidSignatureException(msg);
        }
    }

    public static void verifyDebitBetTypeId(Integer betTypeId) throws InvalidFormatException {
        String betType = BetTypeID.VALID_DEBIT_BET_TYPE_ID.get(betTypeId);
        if (betType == null) {
            throw new InvalidFormatException();
        }
    }

    public static void verifyCreditBetTypeId(Integer betTypeId) throws InvalidFormatException {
        String betType = BetTypeID.VALID_CREDIT_BET_TYPE_ID.get(betTypeId);
        if (betType == null) {
            throw new InvalidFormatException();
        }
    }

    public static Long getOperatorTimestamp(HttpRequestLog httpRequestLog) {
        return Optional.ofNullable(httpRequestLog.getOperatorTimestamp())
                .orElseGet(() -> Optional.ofNullable(httpRequestLog.getOperatorEnd())
                        .orElse(System.currentTimeMillis()));
    }

    public void verifyRollbackAmount(RollbackDto rollbackDto, GameSession gameSession) throws InvalidFormatException, BetNotFoundException, TransactionStillProcessingException {
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String externalTransactionId = rollbackDto.getTransactionId();
        UnsettledBet unsettledBet = null;
        try {
            unsettledBet = unsettledBetService.findBetsForRollback(vendorPlayerId, externalTransactionId);
        } catch (BetNotFoundException betNotFoundException) {
        }
        if (unsettledBet != null && (unsettledBet.getBetAmount().doubleValue() != rollbackDto.getRollbackAmount())) {
            throw new InvalidFormatException();
        }
    }

    public void verifyTokenStatus(Integer status) throws AuthenticationException {
        if (status != Status.ACTIVE.code) {
            throw new AuthenticationException();
        }
    }

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(), gameSession.getPlatformId(), gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }

    public BigDecimal getCurrentBalance(String traceId, String token, HttpRequestLog httpRequestLog) {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            GameSession gameSession = gameSessionService.verifyToken(token);
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
        } catch (Exception exception) {
        }
        return balance;
    }

    public void verifyDebitAfterRollback(Long vendorPlayerId, String externalTransactionId) throws DuplicateExternalTransactionIdException {
        BetNotFoundLog betNotFoundLog = betNotFoundLogService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        // if have data mean have call rollback before
        if (betNotFoundLog != null) {
            throw new DuplicateExternalTransactionIdException();
        }
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        //Will be using betAmount as effectiveTurnover
        return betInfo.getBetAmount();
    }
}
