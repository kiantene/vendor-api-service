package com.nextgen.gameaggregator.custodianseamless.operator.withdraw;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionType;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.DuplicateReferenceIdException;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceTimeoutException;
import com.nextgen.gameaggregator.custodianseamless.operator.deposit.TransferData;
import com.nextgen.gameaggregator.custodianseamless.operator.deposit.TransferDto;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferWalletRequestLog;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHistoryService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.withdraw.WithdrawRequest;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = WalletServiceEndpoints.OPERATOR_ENDPOINT)
@Slf4j
public class WithdrawAction {

    @Autowired
    private TransferHttpService transferHttpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameUrlService gameUrlService;
    @Autowired
    private TransferService transferService;
    @Autowired
    private TransferHistoryService transferHistoryService;

    @Autowired
    private WithdrawRequest withdrawRequest;

    @PostMapping(path = WalletServiceEndpoints.OPERATOR_WITHDRAW)
    public OperatorResponseVo<TransferData> withdraw(HttpServletRequest request) {
        TransferWalletRequestLog transferWalletRequestLog = transferHttpService.start(request);
        OperatorResponseVo<TransferData> responseVo = new OperatorResponseVo<>();

        RawTransferHistory rawTransferHistory = null;
        Currency currency = null;

        try {

            // Retrieve request body in original string format and convert into dto
            String body = transferWalletRequestLog.getRequestBody();
            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            String traceId = dto.getTraceId();
            responseVo.setTraceId(traceId);

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(WalletServiceEndpoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            // 3. Validate the signature
            String signature = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 4.1 Check if Currency exist
            currency = gameUrlService.checkCurrency(dto.getCurrency());
            // 4.2 Check if Agent Currency supported
            AgentCurrency agentCurrency =
                    gameUrlService.checkAgentCurrencySupported(apiCredential.getAgent(), currency);

            //5. validate duplicate traceId request
            transferService.checkTraceIdExists(dto.getTraceId(), apiCredential.getAgent().getId());

            //6. validate duplicate referenceId request
            transferService.checkReferenceIdExists(dto.getReferenceId(), apiCredential.getAgent().getId());

            //7. Check if agent player account exists and is disabled
            AgentPlayer agentPlayer = transferService.checkAgentPlayer(apiCredential.getAgent(), dto.getUsername());

            rawTransferHistory =
                    transferHistoryService.preGenerateRawTransferHistory(dto.getReferenceId(), agentPlayer, currency,
                            TransactionType.WITHDRAWAL.status, dto.getTransferAmount());

            rawTransferHistory = withdrawRequest.call(dto.getTraceId(), rawTransferHistory, transferWalletRequestLog);
        } catch (IllegalArgumentException illegalArgumentException) {
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (DuplicateRequestException duplicateRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_DUPLICATE_REQUEST);

        } catch (DuplicateReferenceIdException duplicateReferenceIdException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_REFERENCE_ID_DUPLICATED);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WRONG_CURRENCY);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_USER_DISABLED);

        } catch (WalletServiceAccessKeyNotFoundException exception) {
            rawTransferHistory.setErrorCode(exception.getWalletStatus());
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);
            exception.printStackTrace();

        } catch (WalletServiceTimeoutException exception) {
            rawTransferHistory.setErrorCode(exception.getWalletStatus());
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);
            exception.printStackTrace();

        } catch (InvalidWalletServiceResponseException exception) {
            rawTransferHistory.setErrorCode(exception.getWalletStatus());
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            exception.printStackTrace();

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            transferHttpService.logError(transferWalletRequestLog, exception);
            exception.printStackTrace();

        } finally {
            if (rawTransferHistory != null && currency != null) {
                TransferData transferData = transferService.saveTransactionHistory(rawTransferHistory, currency);

                if(rawTransferHistory.getErrorCode().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code) ||
                        rawTransferHistory.getErrorCode().equals(ResponseCodes.Status.SC_USER_NOT_EXISTS.code)
                ){
                    responseVo.setResponseCode(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS);
                }

                responseVo.setData(transferData);
            }
            responseVo.setMessage(responseVo.getStatus().description);
            transferWalletRequestLog.setResponseStatus(responseVo.getStatus());

        }

        transferHttpService.end(transferWalletRequestLog, responseVo);
        return responseVo;

    }
}
