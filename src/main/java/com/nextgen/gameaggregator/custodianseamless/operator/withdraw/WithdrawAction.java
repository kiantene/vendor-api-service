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
import com.nextgen.gameaggregator.custodianseamless.service.RequestTrackerService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHistoryService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.withdraw.WithdrawRequest;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.AgentCurrencyService;
import com.nextgen.gameaggregator.service.CurrencyService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = WalletServiceEndpoints.OPERATOR_ENDPOINT)
@Slf4j
public class WithdrawAction {

    private final TransferHttpService transferHttpService;
    private final ValidationService validationService;
    private final CurrencyService currencyService;
    private final AgentCurrencyService agentCurrencyService;
    private final TransferService transferService;
    private final TransferHistoryService transferHistoryService;
    private final WithdrawRequest withdrawRequest;
    private final RequestTrackerService requestTrackerService;

    public WithdrawAction(TransferHttpService transferHttpService,
                          ValidationService validationService,
                          CurrencyService currencyService,
                          AgentCurrencyService agentCurrencyService,
                          TransferService transferService,
                          TransferHistoryService transferHistoryService,
                          WithdrawRequest withdrawRequest,
                          RequestTrackerService requestTrackerService) {

        this.transferHttpService = transferHttpService;
        this.validationService = validationService;
        this.currencyService = currencyService;
        this.agentCurrencyService = agentCurrencyService;
        this.transferService = transferService;
        this.transferHistoryService = transferHistoryService;
        this.withdrawRequest = withdrawRequest;
        this.requestTrackerService = requestTrackerService;
    }

    @PostMapping(path = WalletServiceEndpoints.OPERATOR_WITHDRAW)
    public OperatorResponseVo<TransferData> withdraw(HttpServletRequest request) {
        TransferWalletRequestLog transferWalletRequestLog = transferHttpService.start(request);
        OperatorResponseVo<TransferData> responseVo = new OperatorResponseVo<>();

        RawTransferHistory rawTransferHistory = null;
        Currency currency = null;

        try {

            // Retrieve request body in original string format and convert into dto
            String body = transferWalletRequestLog.getRequestBody();

//            //1. check is duplicate request within the time window
//            requestTrackerService.isNewRequest(body, 300L);

            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            String traceId = dto.getTraceId();
            responseVo.setTraceId(traceId);
            transferWalletRequestLog.setTraceId(traceId);
            transferWalletRequestLog.setRequestType(TransferWalletRequestLog.WITHDRAWAL);
            transferWalletRequestLog.setUsername(dto.getUsername());
            transferWalletRequestLog.setCurrency(dto.getCurrency());
            transferWalletRequestLog.setAmount(dto.getTransferAmount());

            // 2. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 3. Check if api key is valid
            String apiKey = request.getHeader(WalletServiceEndpoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);
            Integer agentId = apiCredential.getAgent().getId();
            transferWalletRequestLog.setAgentId(agentId);

            // 4. validate duplicate traceId request
            transferService.checkTraceIdExists(dto.getTraceId(), agentId);

            // 5. validate duplicate referenceId request
            transferService.checkReferenceIdExists(dto.getReferenceId(), agentId);

            // 6. Validate the signature
            String signature = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 7. Check Agent Status
            validationService.validateAgentStatus(apiCredential.getAgent());

            // 8. check Agent Wallet type and seamless type
            validationService.validateIsCustodianSeamlessAgentWalletType(apiCredential.getAgent());

            // 9.1 Check if Currency exist
            currency = currencyService.getByCode(dto.getCurrency());
            // 9.2 Check if Agent Currency supported
            agentCurrencyService.getByAgentIdAndCurrencyId(agentId, currency.getId());

            // 10. Check if agent player account exists and is disabled
            AgentPlayer agentPlayer = transferService.checkAgentPlayer(agentId, dto.getUsername());

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

        } catch (InvalidWalletTypeException invalidWalletTypeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WALLET_NOT_SUPPORTED);

        } catch (WalletServiceAccessKeyNotFoundException exception) {
            rawTransferHistory.setErrorCode(exception.getWalletStatus());
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);

        } catch (WalletServiceTimeoutException exception) {
            rawTransferHistory.setErrorCode(exception.getWalletStatus());
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);

        } catch (InvalidWalletServiceResponseException exception) {
            rawTransferHistory.setErrorCode(exception.getWalletStatus());
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            transferHttpService.logError(transferWalletRequestLog, exception);

        } finally {
            if (rawTransferHistory != null && currency != null) {
                TransferData transferData = transferService.saveTransactionHistory(rawTransferHistory, currency);

                if (rawTransferHistory.getErrorCode().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code) ||
                        rawTransferHistory.getErrorCode().equals(ResponseCodes.Status.SC_USER_NOT_EXISTS.code)
                ) {
                    responseVo.setResponseCode(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS);
                }

                responseVo.setData(transferData);
            }
            responseVo.setMessage(responseVo.getStatus().description);
            transferWalletRequestLog.setResponseStatus(responseVo.getStatus());
            transferHttpService.end(transferWalletRequestLog, responseVo);
        }

        return responseVo;
    }
}
