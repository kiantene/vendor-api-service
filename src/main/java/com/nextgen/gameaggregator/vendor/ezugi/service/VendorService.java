package com.nextgen.gameaggregator.vendor.ezugi.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.vendor.ezugi.api.rollback.RollbackDto;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private UnsettledBetService unsettledBetService;

    public static String generateGameUrl(String lobbyUrl, String playerGameSessionToken, String operatorId, String languageCode, String gameCode) {
        // form query string
        String loginUrl = lobbyUrl + "?token=" + playerGameSessionToken + "&operatorId=" + operatorId + "&language=" + languageCode + "&openTable=" + gameCode;
        return loginUrl;
    }

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

    public static String generateRequestToken(MultiValueMap<String, String> params, Map<String, String> credentials) throws NoSuchAlgorithmException {
        List<String> values = new ArrayList<>();
        for (String key : params.keySet()) {
            values.add(key + "=" + params.getFirst(key));
        }
        String queryString = credentials.get(Credentials.API_ACCESS) + String.join("&", values);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(queryString.getBytes(StandardCharsets.UTF_8));
        String sha256 = DatatypeConverter.printHexBinary(digest).toLowerCase();
        return sha256;
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

    public void verifyRollbackAmount(RollbackDto rollbackDto, GameSession gameSession) throws InvalidFormatException, BetNotFoundException {
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String externalTransactionId = rollbackDto.getTransactionId();
        UnsettledBet unsettledBet = null;
        unsettledBet = unsettledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        if (unsettledBet != null && (unsettledBet.getBetAmount().doubleValue() != rollbackDto.getRollbackAmount())) {
            throw new InvalidFormatException();
        }
    }

    public void verifyTokenStatus(Integer status) throws AuthenticationException {
        if (status != Status.ACTIVE.code) {
            throw new AuthenticationException();
        }
    }
}
