package com.nextgen.gameaggregator.custodianseamless.operator.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceTimeoutException;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferWalletRequestLog;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.balance.BalanceRequest;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
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
public class BalanceAction {

    @Autowired
    private TransferHttpService transferHttpService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private GameUrlService gameUrlService;

    @Autowired
    private TransferService transferService;
    @Autowired
    private BalanceRequest  balanceRequest;

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

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(WalletServiceEndpoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            // 3. Validate the signature
            String signature = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 4. Check Agent Status
            validationService.validateAgentStatus(apiCredential.getAgent());

            // 5. check Agent Wallet type and seamless type
            validationService.validateIsCustodianSeamlessAgentWalletType(apiCredential.getAgent());

            // 6.1 Check if Currency exist
            Currency currency = gameUrlService.checkCurrency(dto.getCurrency());
            // 6.2 Check if Agent Currency supported
            AgentCurrency agentCurrency =
                    gameUrlService.checkAgentCurrencySupported(apiCredential.getAgent(), currency);

            // 7. validate duplicate traceId request
            transferService.checkTraceIdExists(dto.getTraceId(), apiCredential.getAgent().getId());

            // 8. Check if agent player account exists and is disabled
            AgentPlayer agentPlayer = transferService.checkAgentPlayer(apiCredential.getAgent(), dto.getUsername());

            BalanceData balanceData = balanceRequest.call(traceId, agentPlayer, currency, transferWalletRequestLog);

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
            exception.printStackTrace();

        } catch (WalletServiceTimeoutException exception) {
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INTERNAL_ERROR);
            exception.printStackTrace();

        } catch (InvalidWalletServiceResponseException exception) {
            transferHttpService.logError(transferWalletRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            exception.printStackTrace();

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            transferHttpService.logError(transferWalletRequestLog, exception);
            exception.printStackTrace();

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);
            transferWalletRequestLog.setResponseStatus(responseVo.getStatus());
        }

        transferHttpService.end(transferWalletRequestLog, responseVo);
        return responseVo;
    }
}
