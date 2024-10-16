package com.nextgen.gameaggregator.custodianseamless.operator.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceTimeoutException;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.balance.BalanceRequest;
import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.LoggingService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = WalletServiceEndpoints.OPERATOR_ENDPOINT)
public class BalanceAction {

    private final AgentService agentService;
    private final TransferHttpService transferHttpService;
    private final ValidationService validationService;
    private final CurrencyService currencyService;
    private final AgentCurrencyService agentCurrencyService;
    private final TransferService transferService;
    private final BalanceRequest  balanceRequest;
    private final LoggingService loggingService;


    @PostMapping(path = WalletServiceEndpoints.OPERATOR_BALANCE)
    public OperatorResponseVo<BalanceData> balance(HttpServletRequest request) {

        TransferWalletRequestLog transferWalletRequestLog = transferHttpService.start(request);
        OperatorResponseVo<BalanceData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = transferWalletRequestLog.getRequestBody();
            BalanceDto dto = HttpService.convertJsonToDto(body, BalanceDto.class);
            String traceId = dto.getTraceId();
            responseVo.setTraceId(traceId);
            transferWalletRequestLog.setTraceId(traceId);
            transferWalletRequestLog.setRequestType(TransferWalletRequestLog.BALANCE);
            transferWalletRequestLog.setUsername(dto.getUsername());
            transferWalletRequestLog.setCurrency(dto.getCurrency());

            // 1. Validate all fields in the request object
            loggingService.logStart();
            ValidationUtils.validateRequest(dto);
            loggingService.logProcessTime("balance ｜ ValidationUtils.validateRequest", traceId);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(WalletServiceEndpoints.HEADER_API_KEY);
            loggingService.logStart();
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);
            Integer agentId = apiCredential.getAgent().getId();
            transferWalletRequestLog.setAgentId(agentId);
            loggingService.logProcessTime("balance ｜ validationService.validateApiKey", traceId);

            // 3. validate duplicate traceId request
            loggingService.logStart();
            transferService.checkTraceIdExists(dto.getTraceId(), agentId);
            loggingService.logProcessTime("balance ｜ transferService.checkTraceIdExists", traceId);

            // 4. Validate the signature
            String signature = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
            loggingService.logStart();
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);
            loggingService.logProcessTime("balance ｜ validationService.validateSignature", traceId);

            // 5. Check Agent Status
            Agent agent = agentService.get(agentId);
            validationService.validateAgentStatus(agent);
            loggingService.logProcessTime("balance ｜ validationService.validateAgentStatus", traceId);

            // 6. check Agent Wallet type and seamless type
            loggingService.logStart();
            validationService.validateIsCustodianSeamlessAgentWalletType(agent);
            loggingService.logProcessTime("balance ｜ validationService.validateIsCustodianSeamlessAgentWalletType", traceId);

            // 7.1 Check if Currency exist
            loggingService.logStart();
            Currency currency = currencyService.getByCode(dto.getCurrency());
            loggingService.logProcessTime("balance ｜ currencyService.getByCode", traceId);

            // 7.2 Check if Agent Currency supported
            loggingService.logStart();
            agentCurrencyService.getByAgentIdAndCurrencyId(agentId, currency.getId());
            loggingService.logProcessTime("balance ｜ agentCurrencyService.getByAgentIdAndCurrencyId", traceId);

            // 8. Check if agent player account exists and is disabled
            loggingService.logStart();
            AgentPlayer agentPlayer = transferService.checkAgentPlayer(agentId, dto.getUsername());
            validationService.validateAgentStatus(apiCredential.getAgent());
            loggingService.logProcessTime("balance ｜ validationService.validateAgentStatus", traceId);

            loggingService.logStart();
            BalanceData balanceData = balanceRequest.call(traceId, agentPlayer, currency, transferWalletRequestLog);
            loggingService.logProcessTime("balance ｜ balanceRequest.call", traceId);

            responseVo.setData(balanceData);

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

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_USER_DISABLED);

        } catch (InvalidWalletTypeException invalidWalletTypeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WALLET_NOT_SUPPORTED);

        } catch (WalletServiceAccessKeyNotFoundException exception) {
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);

        } catch (WalletServiceTimeoutException exception) {
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);

        } catch (InvalidWalletServiceResponseException exception) {
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);

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
