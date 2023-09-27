package com.nextgen.gameaggregator.vendor.yesbingo.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Slf4j
public class CancelBetAction {

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

    public ResponseVo cancelBet(HttpRequestLog httpRequestLog, String traceId, String decryptedData) {

        ResponseVo responseVo = new ResponseVo();

        try {

            CancelBetDto dto = HttpService.convertJsonToDto(decryptedData, CancelBetDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUid());

            // Verify request parameters
            this.doVerification(dto, gameSession);

            // Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService);

            // Set Balance and Currency
            responseVo.setBalance(balance);
            responseVo.setStatus(ResponseCodes.SUCCEED);

        } catch (AuthenticationException authenticationException) {
            responseVo.setStatus(ResponseCodes.USER_ID_CANNOT_BE_FOUND);

        } catch (RecordNotFoundException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException noAuthorizedAccessException) {
            responseVo.setStatus(ResponseCodes.NO_AUTHORIZED_ACCESS);

        } catch (InvalidRequestException | JsonProcessingException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            responseVo.setStatus(ResponseCodes.DUPLICATE_TRANSACTIONS);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setStatus(ResponseCodes.DATA_NOT_EXIST);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus().equals(BetStatus.REFUNDED.code)) {
                // if bet already refunded
                responseVo.setBalance(betResultIdempotentViolationException.getBalance());
                responseVo.setStatus(ResponseCodes.SUCCEED);
            } if (betResultIdempotentViolationException.getStatus().equals(BetStatus.SETTLED.code)) {
                // if bet already settled
                responseVo.setStatus(ResponseCodes.DATA_NOT_EXIST);
            } else {
                responseVo.setStatus(ResponseCodes.FAILED);
            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setStatus(ResponseCodes.WORK_IN_PROCESS);

        } catch (InvalidOperatorResponseException exception) {
            responseVo.setStatus(ResponseCodes.WORK_IN_PROCESS);
            httpService.logError(httpRequestLog, exception);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);

        }

        return responseVo;

    }

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(CancelBetDto dto, GameSession gameSession)
            throws
            InvalidPlayerException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

    }
}
