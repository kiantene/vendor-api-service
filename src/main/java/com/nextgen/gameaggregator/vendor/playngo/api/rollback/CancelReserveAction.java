package com.nextgen.gameaggregator.vendor.playngo.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelReserveAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.CANCEL)
    public String balance(HttpServletRequest request) throws InvalidRequestException, JsonProcessingException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        CancelReserveVo cancelReserveVo = new CancelReserveVo();
        XmlMapper xmlMapper = new XmlMapper();
        String cancelReserveVoXml = "";

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            log.info("Playngo Cancel Reserve body: " + body);

            // Convert original request body into commonDto
            CancelReserveDto cancelReserveDto = xmlMapper.readValue(body, CancelReserveDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(cancelReserveDto);

            // Get game session or verify Token
            GameSession gameSession = vendorService.getGameSession(cancelReserveDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, cancelReserveDto);

            // Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelReserveDto, gameSession, vendorService);

            // Construct VO
            cancelReserveVo.setStatusCode(ResponseCodes.OK);
            cancelReserveVo.setReal(balance);

        } catch (InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 GameNotSupportedException |
                 CredentialNotFoundException |
                 JsonProcessingException |
                 InvalidRequestException |
                 NoSuchMethodException |
                 InvocationTargetException |
                 IllegalAccessException internalErrorException) {
            cancelReserveVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (VendorCurrencyNotSupportException | CurrencyNotSupportedException invalidCurrencyException) {
            cancelReserveVo.setStatusCode(ResponseCodes.INVALIDCURRENCY);

        } catch (AuthenticationException authenticationException) {
            cancelReserveVo.setStatusCode(ResponseCodes.SESSIONEXPIRED);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            cancelReserveVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            cancelReserveVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (BetNotFoundException betNotFoundException) {
            cancelReserveVo.setStatusCode(ResponseCodes.OK);
            cancelReserveVo.setExternalTransactionId("");

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if(invalidOperatorResponseException.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                cancelReserveVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);

            } else {
                cancelReserveVo.setStatusCode(ResponseCodes.INTERNAL);
                httpService.logError(httpRequestLog, invalidOperatorResponseException);

            }

        } catch (Exception exception) {
            cancelReserveVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            try {
                cancelReserveVoXml = xmlMapper.writeValueAsString(cancelReserveVo);

            } catch (JsonProcessingException e) {
                cancelReserveVo.setStatusCode(ResponseCodes.INTERNAL);

            }

            cancelReserveVo.setResponseXMLFormat(cancelReserveVoXml);
            httpService.end(httpRequestLog, cancelReserveVo);

        }

        return cancelReserveVoXml;
    }

    private void doValidation(CancelReserveDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, CancelReserveDto dto)
            throws
            CredentialNotFoundException,
            AuthenticationException,
            InvalidPlayerException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidRequestException {

        // Verify vendor's access token
        vendorService.verifyAccessCode(gameSession.getVendorLineId(), dto);

        // Verify Username, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getExternalId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, dto.getGameId());

    }

}
