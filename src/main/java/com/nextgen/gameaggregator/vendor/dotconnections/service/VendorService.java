package com.nextgen.gameaggregator.vendor.dotconnections.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.dotconnections.api.result.EndWagerDto;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    private final SettledBetService settledBetService;
    private final VendorGameService vendorGameService;
    private final UnsettledBetCachingService unsettledBetCachingService;
    private final BetIdempotentLogService betIdempotentLogService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public VendorService(HttpService httpService,
                         WalletService walletService,
                         VendorPlayerService vendorPlayerService,
                         SettledBetService settledBetService,
                         VendorGameService vendorGameService,
                         UnsettledBetCachingService unsettledBetCachingService,
                         BetIdempotentLogService betIdempotentLogService,
                         RedisTemplate<String, Object> redisTemplate) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
        this.settledBetService = settledBetService;
        this.vendorGameService = vendorGameService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.betIdempotentLogService = betIdempotentLogService;
        this.redisTemplate = redisTemplate;
    }


    public static String getSign(String data) {
        String token = DigestUtils.md5Hex(data);
        return token.toUpperCase();
    }

    public static void isSameSignature(String sign, String toVerifySign) throws InvalidSignatureException {
        if (!sign.equals(toVerifySign)) throw new InvalidSignatureException();
    }

    public static String removeDashes(String str) {
        return str.replaceAll("-", "");
    }

    public static String revertToUUID(String uuidString) throws AuthenticationException {

        try {
            StringBuilder sb = new StringBuilder(uuidString);
            sb.insert(8, "-");
            sb.insert(13, "-");
            sb.insert(18, "-");
            sb.insert(23, "-");

            return sb.toString();
        } catch (Exception e) {
            throw new AuthenticationException();
        }
    }

    public ResponseVo getCurrentBalanceResponseVo(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession) {

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        try {
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(walletService.getBalance(traceId, gameSession, httpRequestLog));
            responseVo.setData(responseDataVo);

        } catch (InvalidAgentApiCredentialException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getEffectiveTurnover();
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //will proceed to do cancel bet if the bet is settled. due to endround may come first.
        return false;
    }

    // GA-13072
    // Verify bet status and retrieve the actual vendor game before initializing the game session.
    // Supported endpoints: settle, bet,rollback
    // TODO: support appendWager, promo, freeSpin endpoints
    public VendorGame verifyBetStatusAndGetVendorGameId(String vendorPlayerUsername, String roundId, String externalTransactionId, AtomicBoolean checkBetStatus) throws GameNotSupportedException,
            InvalidPlayerException, BetNotFoundException, BetResultIdempotentViolationException {
        checkBetStatus.set(true);
        Integer vendorGameId;
        UnsettledBet unsettledBet = unsettledBetCachingService.getTop1UnsettledBetWithRoundId(roundId);

        if (unsettledBet == null) {
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerUsername);
            SettledBet settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayer.getId(), externalTransactionId);
            if (settledBet.getStatus() == 1) {
                throw new BetResultIdempotentViolationException();
            }
            vendorGameId = settledBet.getVendorGameId();

        } else {
            vendorGameId = unsettledBet.getVendorGameId();
        }
        return vendorGameService.getByVendorGameIdIgnoreStatus(vendorGameId);
    }

    public void verifyBetStatus(EndWagerDto dto) throws BetNotFoundException, BetResultIdempotentViolationException {
        UnsettledBet unsettledBet = unsettledBetCachingService.getTop1UnsettledBetWithRoundId(dto.getRoundId());

        if (unsettledBet == null) {
            RawBetIdempotentLog rawBetIdempotentLog = betIdempotentLogService.checkExists(dto.getVendorBetId(), dto.getRoundId(), dto.getBrandUid());
            if (rawBetIdempotentLog == null) {
                throw new BetNotFoundException();
            } else {
                throw new BetResultIdempotentViolationException(rawBetIdempotentLog);
            }
        }
    }

    public void scheduleTempSessionTokenDeletion(String vendorPlayerUsername, String roundId) {
        String key = "TempSessionToken::" + vendorPlayerUsername + "," + roundId;
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
    }

    public void errorResponseBetNotFound(CommonDto dto, ResponseVo responseVo) {
        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBrandUid(dto.getBrandUid());
        responseDataVo.setCurrency(dto.getCurrency());
        responseDataVo.setBalance(BigDecimal.ZERO);
        responseVo.setData(responseDataVo);
    }
}
