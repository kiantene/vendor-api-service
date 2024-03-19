package com.nextgen.gameaggregator.vendor.habanero.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.RawBetResultLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.BetResultLogService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private BetResultLogService betResultLogService;

    public static String generateSHA256Hash(String input) throws InvalidEncryptionException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(input.getBytes());
            StringBuilder stringBuilder = new StringBuilder();

            for (byte hashByte : hashBytes) {
                stringBuilder.append(String.format("%02x", hashByte));
            }

            return stringBuilder.toString();
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public static boolean isValidDateString(String timestamp) {
        try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static Long dateTimeConvert(String rawDateTime) {

        //convert date time string to timestamp
        Long timestamp = null;
        if (rawDateTime != null) {
            LocalDateTime localDateTime = LocalDateTime.parse(rawDateTime, DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
            timestamp = zonedDateTime.toInstant().toEpochMilli();
        }
        return timestamp;

    }

    public void unsettledBetIdempotentCheck(GameSession gameSession, String vendorBetId, String roundId)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        UnsettledBet unsettledBet = null;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        try {
            unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
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

    public void betResultIdempotentCheck(GameSession gameSession, String externalTransactionId, String roundId)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        String vendorGameId = gameSession.getVendorGameId().toString();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        RawBetResultLog rawBetResultLog = betResultLogService.checkExists(externalTransactionId, roundId, vendorGameId, vendorPlayerId);

        if (rawBetResultLog != null) {
            Integer operatorStatus = rawBetResultLog.getOperatorStatus();

            // throw idempotent exception if status is processing or success
            if (operatorStatus.equals(operatorStatusProcessing)) {
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(operatorStatusSuccess)) {
                throw new BetResultIdempotentViolationException(rawBetResultLog);

            } else { // when bet result found and operator status is error
                //no action
            }
        } else { // when bet result not found
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
            settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);

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
