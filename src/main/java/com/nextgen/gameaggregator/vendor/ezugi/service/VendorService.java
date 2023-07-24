package com.nextgen.gameaggregator.vendor.ezugi.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.entity.VendorGameCode;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import com.nextgen.gameaggregator.vendor.ezugi.api.rollback.RollbackDto;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private VendorGameCodeService vendorGameCodeService;

    public static void verifyHash(String secretKey, String data, String hashKey) throws InvalidKeyException, NoSuchAlgorithmException, InvalidSignatureException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(),
                "HmacSHA256");
        sha256_HMAC.init(secret_key);
        String generatedHash = Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes()));
        if (!hashKey.equals(generatedHash)) {
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

    public void verifyRollbackAmount(RollbackDto rollbackDto, GameSession gameSession) throws InvalidFormatException, BetNotFoundException, TransactionStillProcessingException {
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String externalTransactionId = rollbackDto.getTransactionId();
        UnsettledBet unsettledBet = null;
        unsettledBet = unsettledBetService.findBetsForRollback(vendorPlayerId, externalTransactionId);
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
}
