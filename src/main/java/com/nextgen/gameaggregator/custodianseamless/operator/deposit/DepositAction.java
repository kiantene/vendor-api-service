package com.nextgen.gameaggregator.custodianseamless.operator.deposit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionType;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.DuplicateReferenceIdException;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferData;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferDto;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferWalletRequestLog;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHistoryService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferHttpService;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.deposit.DepositRequest;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.KafkaService;
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
public class DepositAction {

    @Autowired
    private TransferHttpService transferHttpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameUrlService gameUrlService;
    @Autowired
    private TransferHistoryService transferHistoryService;
    @Autowired
    private TransferService transferService;
    @Autowired
    private DepositRequest depositRequest;
    @Autowired
    private KafkaService kafkaService;

    @PostMapping(path = WalletServiceEndpoints.OPERATOR_DEPOSIT)
    public OperatorResponseVo<TransferData> deposit(HttpServletRequest request) {

        TransferWalletRequestLog transferWalletRequestLog = transferHttpService.start(request);
        OperatorResponseVo<TransferData> responseVo = new OperatorResponseVo<>();


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
            Currency currency = gameUrlService.checkCurrency(dto.getCurrency());
            // 4.2 Check if Agent Currency supported
            AgentCurrency agentCurrency =
                    gameUrlService.checkAgentCurrencySupported(apiCredential.getAgent(), currency);

            //5. validate duplicate traceId request
            transferHistoryService.checkTraceIdExists(dto.getTraceId(), apiCredential.getAgent().getId());

            //6. validate duplicate referenceId request
            RawTransferHistory  rawTransferHistory= transferHistoryService.checkTransactionExists(dto.getReferenceId(), apiCredential.getAgent().getId());
            if(rawTransferHistory != null){
                throw new DuplicateReferenceIdException("referenceId :" + dto.getReferenceId() + " existing within 7 days ");
            }

            AgentPlayer agentPlayer = transferService.checkAgentPlayer(apiCredential.getAgent(), dto.getUsername());

            rawTransferHistory =
                    transferHistoryService.preGenerateRawTransferHistory(dto.getReferenceId(), agentPlayer, currency,
                            TransactionType.DEPOSIT.status, dto.getTransferAmount());


            rawTransferHistory = depositRequest.call(dto.getTraceId(), rawTransferHistory);
            transferHistoryService.updateRawTransferHistory(rawTransferHistory);
            transferHistoryService.saveRawTransferHistory(rawTransferHistory);

            TransferData transferData = new TransferData(rawTransferHistory, currency.getCode());

            //8. send to process transfer history kafka topic
            kafkaService.produceTransferHistory(rawTransferHistory);

            responseVo.setData(transferData);


        } catch (IllegalArgumentException illegalArgumentException) {
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
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


//        } catch (Exception exception) {
//            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
//            httpService.logError(httpRequestLog, exception);
//            exception.printStackTrace();

        } catch (InvalidWalletServiceResponseException e) {
            throw new RuntimeException(e);
        } catch (HttpResponseStatusCodeException e) {
            throw new RuntimeException(e);
        } catch (WalletServiceAccessKeyNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } finally {
            responseVo.setMessage(responseVo.getStatus().description);
            //transferWalletRequestLog.setOperatorResponseStatus(responseVo.getStatus());

        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
