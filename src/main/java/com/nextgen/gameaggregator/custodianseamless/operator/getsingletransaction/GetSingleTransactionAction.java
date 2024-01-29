package com.nextgen.gameaggregator.custodianseamless.operator.getsingletransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.TransferHistoryNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferWalletRequestLog;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
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
public class GetSingleTransactionAction {

    @Autowired
    private TransferHttpService transferHttpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameUrlService gameUrlService;
    @Autowired
    private TransferService transferService;

    @PostMapping(path = WalletServiceEndpoints.OPERATOR_GET_SINGLE_TRANSACTION)
    public OperatorResponseVo<GetSingleTransactionData> getsingletransaction(HttpServletRequest request) {

        TransferWalletRequestLog transferWalletRequestLog = transferHttpService.start(request);
        OperatorResponseVo<GetSingleTransactionData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = transferWalletRequestLog.getRequestBody();
            GetSingleTransactionDto dto = HttpService.convertJsonToDto(body, GetSingleTransactionDto.class);
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
            Currency currency = gameUrlService.checkCurrency(dto.getCurrency());
            // 4.2 Check if Agent Currency supported
            AgentCurrency agentCurrency =
                    gameUrlService.checkAgentCurrencySupported(apiCredential.getAgent(), currency);

            //5. validate duplicate traceId request
            transferService.checkTraceIdExists(dto.getTraceId(), apiCredential.getAgent().getId());

            RawTransferHistory transferHistory =
                    transferService.getTransferHistoryByReferenceId(dto.getReferenceId(), apiCredential.getAgent().getId(),
                            currency, dto.getUsername());

            GetSingleTransactionData getSingleTransactionData = new GetSingleTransactionData(transferHistory, currency);

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

        } catch (TransferHistoryNotFoundException transferHistoryNotFoundException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_TRANSACTION_DOES_NOT_EXIST);

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
