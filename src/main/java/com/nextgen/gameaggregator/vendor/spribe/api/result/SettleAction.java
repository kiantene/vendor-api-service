package com.nextgen.gameaggregator.vendor.spribe.api.result;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.vo.DataVo;
import com.nextgen.gameaggregator.vendor.spribe.vo.ResponseVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class SettleAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;
    
    @PostMapping(path = Endpoints.DEPOSIT)
    public ResponseVo settle(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        DataVo data = new DataVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            SettleDto dto = HttpService.convertJsonToDto(body, SettleDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUser_id());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal oldBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Send bet request to Operator
            ResultType resultType = getResultType(dto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // 7. Set response data
            data.setOperator_tx_id(traceId);
            data.setNew_balance(balance);
            data.setOld_balance(oldBalance);
            data.setUser_id(gameSession.getVendorPlayerUsername());
            data.setCurrency(gameSession.getVendorCurrencyCode());
            data.setProvider(dto.getProvider());
            data.setProvider_tx_id(dto.getProvider_tx_id());
            vo.setErrorCode(ErrorCodes.SUCCESS);
            vo.setData(data);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorCode(ErrorCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidAgentApiCredentialException | VendorCurrencyNotSupportException | 
            InvalidRequestException | DisabledVendorLineException | DisabledAgentPlayerException | DisabledGameException | 
            BetNotFoundException | InvalidOperatorResponseException | MergedBetDataIntegrityException | 
            InsufficientBalanceException | TransactionStillProcessingException internalErrorExeption) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, internalErrorExeption);

        } catch (Exception exception) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, SettleDto dto, GameSession gameSession) throws InvalidPlayerException, 
        DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException {

        validationService.validateEligibleBet(gameSession, dto.getUser_id());
    }
    
    private ResultType getResultType(SettleDto dto) {

        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal zero = BigDecimal.ZERO;

        if (dto.getWinAmount().compareTo(zero) > 0) { 
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }
}
