package com.nextgen.gameaggregator.vendor.yesbingo.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Formats;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@Slf4j
public class GameDetailResultAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    public ResponseVo gameDetailResult(HttpRequestLog httpRequestLog, String traceId, String decryptedData) {

        ResponseVo responseVo = new ResponseVo();

        try {

            GameDetailResultDto dto = HttpService.convertJsonToDto(decryptedData, GameDetailResultDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getUid(), dto.getGameId());

            // Verify data
            this.doVerification(dto, gameSession);

            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), true);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // Set Balance and Currency
            responseVo.setBalance(balance);
            responseVo.setStatus(ResponseCodes.SUCCEED);

        } catch (InvalidAgentApiCredentialException | AuthenticationException | InvalidPlayerException |
                 CurrencyNotSupportedException | DisabledAgentPlayerException | DisabledGameException |
                 DisabledVendorLineException | GameNotSupportedException noAuthorizedAccessException) {
            responseVo.setStatus(ResponseCodes.NO_AUTHORIZED_ACCESS);
        } catch (InvalidRequestException | JsonProcessingException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);
        } catch (DateTimeParseException dateTimeParseException) {
            responseVo.setStatus(ResponseCodes.WRONG_DATE_SECOND_FORMAT);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setStatus(ResponseCodes.DUPLICATE_TRANSACTIONS);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setStatus(ResponseCodes.CASH_BALANCE_NOT_ENOUGH);
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            // 6001-The system is busy (vendor proceeds to cancel the bet)
            responseVo.setStatus(ResponseCodes.SYSTEM_BUSY);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;

    }

    private void doValidation(GameDetailResultDto dto) throws InvalidRequestException, DateTimeParseException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT);
        formatter.parse(dto.getGameDate());
        formatter.parse(dto.getReportDate());
        formatter.parse(dto.getLastModifyTime());

        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameDetailResultDto dto, GameSession gameSession)
            throws InvalidPlayerException, CurrencyNotSupportedException, DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify Game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
    }
}
