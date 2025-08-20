package com.nextgen.gameaggregator.vendor.dreamgaming.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.bet.AppendDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.rollback.RollbackDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Formats;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.TransferType;
import com.nextgen.gameaggregator.vendor.dreamgaming.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private final GameSessionService gameSessionService;

    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String signGenerator(Map<String, String> credentials, String timeStamp) {
        return md5Generator(credentials.get(Credentials.AGENT_ID) + credentials.get(Credentials.API_KEY) + timeStamp);
    }

    public static String removeLeadingZero(String input) {
        return input.replaceAll(Formats.JSON_LEADING_ZERO, "$1");
    }

    public GameSession checkGameSession(String traceId, BetDto betDto) throws
            VendorCurrencyNotSupportException,
            InvalidPlayerException,
            AuthenticationException,
            GameNotSupportedException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getMember().getUsername().toLowerCase());
            gameSession = this.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getDetailDto().getTableId(), gameSession);
        } catch (AuthenticationException authenticationException) {
            int type = betDto.getType();

            if (type == TransferType.PAYOUT || type == TransferType.APPEND) {
                gameSession = gameSessionService.generateNewSessionToken(betDto.getMember().getUsername().toLowerCase());
                gameSessionService.updateByVendorGameCode(gameSession, betDto.getDetailDto().getTableId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            } else {
                throw authenticationException;
            }
        }
        return gameSession;
    }

    public GameSession checkGameSession(String traceId, RollbackDto rollbackDto) throws
            VendorCurrencyNotSupportException,
            InvalidPlayerException,
            GameNotSupportedException,
            AuthenticationException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(rollbackDto.getMember().getUsername().toLowerCase());
            gameSession = this.verifyAndRegenerateNewVendorGameCodeForGameSession(rollbackDto.getDetailDto().getTableId(), gameSession);
        } catch (AuthenticationException authenticationException) {
            int type = rollbackDto.getType();
            if (type == TransferType.BET || type == TransferType.PAYOUT || type == TransferType.APPEND) {
                gameSession = gameSessionService.generateNewSessionToken(rollbackDto.getMember().getUsername().toLowerCase());
                gameSessionService.updateByVendorGameCode(gameSession, rollbackDto.getDetailDto().getTableId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            } else {
                throw authenticationException;
            }
        }
        return gameSession;
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        BigDecimal effectiveTurnover = betInfo.getEffectiveTurnover();

        // Compare by numeric value only (ignores scale)
        // if -1 will set it to unsettled bet amount because availableBet param have no value.
        if (effectiveTurnover.compareTo(BigDecimal.valueOf(-1)) == 0) {
            effectiveTurnover = betInfo.getBetAmount();
        }

        return effectiveTurnover;
    }

    public void setVoBalance(HttpRequestLog httpRequestLog, BigDecimal balance, BetDto betDto, ResponseVo vo, AppendDto appendDto) throws JsonProcessingException {
        String status = null;
        if (httpRequestLog.getOperatorResponse() != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(httpRequestLog.getOperatorResponse());
            status = jsonNode.path("status").asText();
        }

        if (status != null && status.equals("SC_OK")) {
            switch (betDto.getType()) {
                case TransferType.BET:
                    vo.getMember().setBalance(balance.add(betDto.getBetAmount()));
                    break;
                case TransferType.PAYOUT:
                    vo.getMember().setBalance(balance.subtract(betDto.getWinAmount()));
                    break;
                case TransferType.APPEND:
                    vo.getMember().setBalance(balance.subtract(appendDto.getAdjustmentAmount()));
                    break;
                default:
                    vo.getMember().setBalance(balance);
            }
        } else {
            vo.getMember().setBalance(balance);
        }
    }

    public void setVoBalance(HttpRequestLog httpRequestLog, BigDecimal balance, RollbackDto rollbackDto, BetDto betDto, ResponseVo vo, AppendDto appendDto) throws JsonProcessingException {
        String status = null;
        if (httpRequestLog.getOperatorResponse() != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(httpRequestLog.getOperatorResponse());
            status = jsonNode.path("status").asText();
        }

        if (status != null && status.equals("SC_OK")) {
            switch (rollbackDto.getType()) {
                case TransferType.BET:
                    vo.getMember().setBalance(balance.subtract(rollbackDto.getMember().getAmount().abs()));
                    break;
                case TransferType.PAYOUT:
                    vo.getMember().setBalance(balance.subtract(betDto.getWinAmount()));
                    break;
                case TransferType.APPEND:
                    vo.getMember().setBalance(balance.subtract(appendDto.getAdjustmentAmount()));
                    break;
                default:
                    vo.getMember().setBalance(balance);
            }
        } else {
            vo.getMember().setBalance(balance);
        }
    }
}
