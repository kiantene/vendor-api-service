package com.nextgen.gameaggregator.custodianseamless.operator.getsingletransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.TransferHistoryNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
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
public class GetSingleTransactionAction {

    private final TransferHttpService transferHttpService;
    private final ValidationService validationService;
    private final CurrencyService currencyService;
    private final AgentCurrencyService agentCurrencyService;
    private final TransferService transferService;

    public GetSingleTransactionAction(TransferHttpService transferHttpService,
                                      ValidationService validationService,
                                      CurrencyService currencyService,
                                      AgentCurrencyService agentCurrencyService,
                                      TransferService transferService) {

        this.transferHttpService = transferHttpService;
        this.validationService = validationService;
        this.currencyService = currencyService;
        this.agentCurrencyService = agentCurrencyService;
        this.transferService = transferService;
    }

    @PostMapping(path = WalletServiceEndpoints.OPERATOR_GET_SINGLE_TRANSACTION)
    public OperatorResponseVo<GetSingleTransactionData> getSingleTransaction(HttpServletRequest request) {

        TransferWalletRequestLog transferWalletRequestLog = transferHttpService.start(request);
        OperatorResponseVo<GetSingleTransactionData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = transferWalletRequestLog.getRequestBody();
            GetSingleTransactionDto dto = HttpService.convertJsonToDto(body, GetSingleTransactionDto.class);
            String traceId = dto.getTraceId();
            responseVo.setTraceId(traceId);
            transferWalletRequestLog.setTraceId(traceId);
            transferWalletRequestLog.setRequestType(TransferWalletRequestLog.GET_TXN);
            transferWalletRequestLog.setUsername(dto.getUsername());
            transferWalletRequestLog.setCurrency(dto.getCurrency());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(WalletServiceEndpoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);
            Integer agentId = apiCredential.getAgent().getId();
            transferWalletRequestLog.setAgentId(agentId);

            // 3. validate duplicate traceId request
            transferService.checkTraceIdExists(dto.getTraceId(), agentId);

            // 4. Validate the signature
            String signature = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 5. Check Agent Status
            validationService.validateAgentStatus(apiCredential.getAgent());

            // 6. check Agent Wallet type and seamless type
            validationService.validateIsCustodianSeamlessAgentWalletType(apiCredential.getAgent());

            // 7.1 Check if Currency exist
            Currency currency = currencyService.getByCode(dto.getCurrency());
            // 7.2 Check if Agent Currency supported
            agentCurrencyService.getByAgentIdAndCurrencyId(agentId, currency.getId());

            TransferHistory transferHistory =
                    transferService.getTransferHistoryByReferenceId(dto.getReferenceId(), apiCredential.getAgent().getId(),
                            currency, dto.getUsername());

            GetSingleTransactionData getSingleTransactionData = new GetSingleTransactionData(transferHistory, currency);
            transferWalletRequestLog.setAmount(transferHistory.getTransferAmount());
            transferWalletRequestLog.setResponseBody(new Gson().toJson(transferHistory));

            responseVo.setData(getSingleTransactionData);

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

        } catch (InvalidCurrencyException invalidCurrencyException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WRONG_CURRENCY);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (InvalidWalletTypeException invalidWalletTypeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WALLET_NOT_SUPPORTED);

        } catch (TransferHistoryNotFoundException transferHistoryNotFoundException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_TRANSACTION_DOES_NOT_EXIST);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            transferHttpService.logError(transferWalletRequestLog, exception);

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);
            transferWalletRequestLog.setResponseStatus(responseVo.getStatus());
            transferHttpService.end(transferWalletRequestLog, responseVo);
        }

        return responseVo;
    }
}
