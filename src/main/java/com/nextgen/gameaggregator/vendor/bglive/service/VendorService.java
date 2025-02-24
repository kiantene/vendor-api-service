package com.nextgen.gameaggregator.vendor.bglive.service;


import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.UnsettledBetCachingService;
import com.nextgen.gameaggregator.vendor.bglive.constant.QueryStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Getter
@Setter
public class VendorService extends BaseVendorService {

    private GameSessionService gameSessionService;
    private UnsettledBetCachingService unsettledBetCachingService;
    private SettledBetService settledBetService;

    @Autowired
    public VendorService(GameSessionService gameSessionService,
                         UnsettledBetCachingService unsettledBetCachingService,
                         SettledBetService settledBetService) {
        this.gameSessionService = gameSessionService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.settledBetService = settledBetService;
    }

    public static String encryptCreateUserMd5Key(String random, String snCode, String secretCode) throws InvalidFormatException {
        try {
            String combined = random + snCode + secretCode;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt create user MD5 Fail");
        }
    }

    public static String encryptLoginMd5Key(String random, String snCode, String loginId, String secretCode) throws InvalidFormatException {
        try {
            String combined = random + snCode + loginId + secretCode;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt login MD5 Fail");
        }
    }

    public static String encryptBetMd5Key(String random, String snCode, String loginId, String amount, String secretCode) throws InvalidFormatException {
        try {
            String combined = random + snCode + loginId + amount + secretCode;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt bet MD5 Fail");
        }
    }

    public static String generateSecretCode(String password) throws InvalidFormatException {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            byte[] sha1Hash = sha1Digest.digest(password.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(sha1Hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidFormatException("SHA-1 algorithm not found");
        }
    }

    public static String getGameCode(String input) throws InvalidFormatException {
        if (input != null && input.length() >= 5) {
            return input.substring(2, 5);
        } else {
            throw new InvalidFormatException("Get game code error");
        }
    }

    public static boolean isDoublePlay(long playId) {
        String hexPlayId = Long.toHexString(playId);
        char lastChar = hexPlayId.charAt(hexPlayId.length() - 1);
        int lastDigit = Character.digit(lastChar, 16);
        return lastDigit == 2 || lastDigit == 8 || lastDigit == 0;
    }

    public Integer unsettledBetIdempotentCheck(String roundId)
            throws BetNotFoundException {

        UnsettledBet unsettledBet =
                unsettledBetCachingService.getTop1UnsettledBetWithRoundId(roundId);
        if (unsettledBet != null) {
            return QueryStatus.UNSETTLE_BET;
        } else {
            return QueryStatus.NO_BET;
        }
    }

    public Integer settledBetIdempotentCheck(GameSession gameSession, String externalId)
            throws BetNotFoundException {

        SettledBet settledBet =
                settledBetService.getByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), externalId);

        BigDecimal winLoss = settledBet.getWinLoss();
        if (winLoss.compareTo(BigDecimal.ZERO) > 0) {
            return QueryStatus.SETTLE_WIN;
        } else if (winLoss.compareTo(BigDecimal.ZERO) < 0) {
            return QueryStatus.SETTLE_LOSE;
        } else {
            return QueryStatus.SETTLE_TIE;
        }
    }
}