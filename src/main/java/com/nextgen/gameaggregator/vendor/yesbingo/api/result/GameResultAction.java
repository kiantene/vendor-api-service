package com.nextgen.gameaggregator.vendor.yesbingo.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.GameTypes;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@Slf4j
public class GameResultAction {

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

    public void gameResult(HttpRequestLog httpRequestLog, String traceId, String decryptedData, ResponseVo responseVo) {

        try {

            GameResultDto dto = HttpService.convertJsonToDto(decryptedData, GameResultDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUid());

            // Verify data
            this.doVerification(dto, gameSession);

            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // Set Balance and Currency
            responseVo.setBalance(balance);
            responseVo.setStatus(ResponseCodes.SUCCEED);

        } catch (AuthenticationException authenticationException) {
            responseVo.setStatus(ResponseCodes.USER_ID_CANNOT_BE_FOUND);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 GameNotSupportedException noAuthorizedAccessException) {
            responseVo.setStatus(ResponseCodes.NO_AUTHORIZED_ACCESS);
            httpService.logError(httpRequestLog, noAuthorizedAccessException);

        } catch (InvalidRequestException invalidRequestException) {
            if (invalidRequestException.getValidation() != null) {
                String violation = invalidRequestException.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCodes.PARAMETER_INPUT_ERROR); // if there's no value, set it to the default invalid request parameter
                responseVo.setStatus(violation);

            } else {
                responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);

            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (JsonProcessingException | CurrencyNotSupportedException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);
            httpService.logError(httpRequestLog, parameterInputErrorException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setStatus(ResponseCodes.DUPLICATE_TRANSACTIONS);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setStatus(ResponseCodes.CASH_BALANCE_NOT_ENOUGH);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            // 9017 Work in process (vendor will retry)
            responseVo.setStatus(ResponseCodes.WORK_IN_PROCESS);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setStatus(ResponseCodes.FAILED, ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.DATA_NOT_EXIST));
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setStatus(ResponseCodes.WORK_IN_PROCESS);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);

        }

    }

    private void doValidation(GameResultDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        if (dto.getGType() == GameTypes.SLOT && (dto.getJackpotWin() == null || dto.getJackpotContribute() == null) ||
                (dto.getGType() == GameTypes.BINGO && (dto.getPlaySeq() == null || dto.getRound() == null))
        ) {
            throw new InvalidRequestException();
        }

    }

    private void doVerification(GameResultDto dto, GameSession gameSession)
            throws
            AuthenticationException,
            InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException {

        // Verify vendor gameCode, currency and platform
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

}
