package com.nextgen.gameaggregator.vendor.bglive.service;


import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bglive.api.settlement.OrdersDto;
import com.nextgen.gameaggregator.vendor.bglive.constant.QueryStatus;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service("bgliveVendorService")
@Getter
@Setter
public class VendorService extends BaseVendorService {

    private final WalletRequestService walletRequestService;
    private final BetActionLogService betActionLogService;
    private GameSessionService gameSessionService;
    private UnsettledBetCachingService unsettledBetCachingService;
    private SettledBetService settledBetService;
    private BetNotFoundLogService betNotFoundLogService;

    @Autowired
    public VendorService(GameSessionService gameSessionService,
                         UnsettledBetCachingService unsettledBetCachingService,
                         SettledBetService settledBetService, WalletRequestService walletRequestService, BetActionLogService betActionLogService) {
        this.gameSessionService = gameSessionService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.settledBetService = settledBetService;
        this.walletRequestService = walletRequestService;
        this.betActionLogService = betActionLogService;
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

    public static <T> List<T> processMultipleDataResponds(List<CompletableFuture<T>> futureList) {
        List<CompletableFuture<T>> futuresWithTimeout = futureList.stream()
                .map(future -> future.orTimeout(5L, TimeUnit.SECONDS)
                        .exceptionally(ex -> null))
                .toList();

        CompletableFuture.allOf(futuresWithTimeout.toArray(new CompletableFuture[0])).join();

        return futuresWithTimeout.stream()
                .map(CompletableFuture::join)
                .toList();
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

    public BigDecimal checkSettleResponseAndReturnBalance(List<CompletableFuture<ResultVo>> resultVoList) throws
            InsufficientBalanceException,
            BetNotFoundException {
        List<ResultVo> resultList = processMultipleDataResponds(resultVoList);

        boolean hasValidResult = false;

        for (ResultVo resultVo : resultList) {
            if (resultVo == null) {
                continue;
            }
            hasValidResult = true;

            if (resultVo.getAvailableAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException();
            }
        }

        if (!hasValidResult) {
            throw new BetNotFoundException("All results are null");
        }
        //get latest timestamp
        return resultList.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(ResultVo::getTimestamp))
                .map(ResultVo::getAvailableAmount)
                .orElse(null);
    }

    public BigDecimal checkResponseAndReturnBalance(List<CompletableFuture<ResultVo>> resultVoList, Boolean processFailed) throws InsufficientBalanceException {
        List<ResultVo> resultList = processMultipleDataResponds(resultVoList);

        for (ResultVo resultVo : resultList) {
            if (resultVo == null) {
                return null;
            } else if (resultVo.getAvailableAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException();
            }
        }
        // Find the latest process bet event
        ResultVo resultVo = resultList.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(ResultVo::getTimestamp))
                .orElse(null);

        if (resultVo != null) {
            return resultVo.getAvailableAmount();
        }
        return null;
    }

    public void dataDebitMapper(WalletRequest walletRequest, com.nextgen.gameaggregator.vendor.bglive.api.bet.OrdersDto ordersDto, GameSession gameSession) {
        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(ordersDto.getExternalTransactionId());
        walletRequest.setRoundId(ordersDto.getRoundId());
        walletRequest.setVendorGameCode(ordersDto.getGameId());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(ordersDto.getVendorBetId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        BigDecimal amount = ordersDto.getAmount().abs();
        walletRequest.setTransferAmount(amount);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
    }

    public void dataCreditMapper(WalletRequest walletRequest, OrdersDto ordersDto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        walletRequest.setExternalTransactionId(ordersDto.getRoundId());
        walletRequest.setRoundId(ordersDto.getRoundId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(ordersDto.getVendorBetId());
        walletRequest.setTakeAll(0);
        BigDecimal amount = ordersDto.getAmount().abs();
        walletRequest.setTransferAmount(amount);
        walletRequest.setBetAmount(amount);
        ResultType resultType = this.calculateResultType(null, amount, ordersDto.getJackpotAmount(),
                false);
        walletRequest.setWinAmount(amount);
        walletRequest.setEffectiveTurnover(BigDecimal.ZERO);
        walletRequest.setJackpotAmount(ordersDto.getJackpotAmount());
        walletRequest.setResultType(resultType.code);
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setVendorSettleTime(System.currentTimeMillis());
    }


}