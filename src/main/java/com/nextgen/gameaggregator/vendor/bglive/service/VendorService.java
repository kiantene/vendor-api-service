package com.nextgen.gameaggregator.vendor.bglive.service;


import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.UnsettledBetCachingService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    public void unsettledBetIdempotentCheck(String roundId)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        UnsettledBet unsettledBet = null;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        try {
            unsettledBet = UnsettledBetCachingService.getTop1UnsettledBetWithRoundId(roundId);
            Integer operatorStatus = unsettledBet.getOperatorStatus();

            // throw idempotent exception if status is processing or success
            if (operatorStatus.equals(operatorStatusProcessing)) {
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(operatorStatusSuccess)) {
                throw new BetResultIdempotentViolationException(unsettledBet);

            } else { // when bet result found and operator status is error, throw transaction still processing
                throw new TransactionStillProcessingException();
            }
        } catch (BetNotFoundException betNotFoundException) {
            //no action
        }
    }

    public void settledBetIdempotentCheck(GameSession gameSession, String vendorBetId, String roundId)
            throws BetResultIdempotentViolationException, TransactionStillProcessingException {

        Integer vendorId = gameSession.getVendorId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        SettledBet settledBet = null;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        try {
            // Add retry to find settled bet, because DNC request (debit & credit) and Query request very frequently
            settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerIdRetry(vendorBetId, roundId, vendorId, vendorPlayerId);

            if (settledBet != null) { // duplicate request found in settled_bet
                Integer operatorStatus = settledBet.getOperatorStatus();
                // throw idempotent exception if status is processing or success
                if (operatorStatus.equals(operatorStatusProcessing)) {
                    throw new TransactionStillProcessingException();

                } else if (operatorStatus.equals(operatorStatusSuccess)) {
                    throw new BetResultIdempotentViolationException(settledBet);

                } else { // when bet result found and operator status is error
                    //no action
                }
            }
        } catch (BetNotFoundException betNotFoundException) {
            //no action
        }
    }

}