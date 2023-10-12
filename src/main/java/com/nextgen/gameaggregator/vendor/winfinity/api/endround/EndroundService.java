package com.nextgen.gameaggregator.vendor.winfinity.api.endround;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.service.VendorService;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EndroundService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private HttpService httpService;

    public ResponseVo endround(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();
        
        try {
            // Convert original request body into dto
            EndroundDto dto = HttpService.convertJsonToDto(body, EndroundDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, ResultType.END, vendorService, httpRequestLog);
            
            vo.setDataVo(traceId, balance);

        } catch (JsonProcessingException | TransactionStillProcessingException | InvalidRequestException badRequestException) {
            httpService.logError(httpRequestLog, badRequestException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorVo(ErrorCodes.WRONG_SESSION);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorVo(ErrorCodes.NOT_ENOUGH_FUND);
            
        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            vo.setErrorVo(ErrorCodes.PAYIN_TRANS_NOT_FOUND);

        } catch (MergedBetDataIntegrityException | InvalidOperatorResponseException | InvalidAgentApiCredentialException | 
                DisabledVendorLineException | DisabledAgentPlayerException unknownErrorException) {
            httpService.logError(httpRequestLog, unknownErrorException);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            vo.setErrorVo(ErrorCodes.TRANS_ALREADY_EXISTS);

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            vo.setErrorVo(ErrorCodes.GAME_NOT_AVAILABLE);

        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            log.error("Other exception encountered: " + exception.getMessage());
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        }

        return vo;
    }

    private void doValidation(EndroundDto dto) throws InvalidRequestException {
       // General validation
       ValidationUtils.validateRequest(dto);
    }

    private void doVerification(EndroundDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
