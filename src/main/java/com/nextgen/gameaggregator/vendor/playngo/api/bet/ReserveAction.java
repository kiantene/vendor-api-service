package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ReserveAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.RESERVE)
    public String reserve(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ReserveVo reserveVo = new ReserveVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = null;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            ReserveDto reserveDto = xmlMapper.readValue(body, ReserveDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(reserveDto);

            // Get game session or verify Token
            gameSession = vendorService.getGameSession(reserveDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, reserveDto);

            // Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, reserveDto, body, httpRequestLog);

            // Construct VO
            reserveVo.setStatusCode(ResponseCodes.OK);
            reserveVo.setReal(betEvent.getLastBalance());

        } catch (InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 GameNotSupportedException |
                 CredentialNotFoundException |
                 JsonProcessingException |
                 InvalidRequestException |
                 NoSuchMethodException |
                 InvocationTargetException |
                 IllegalAccessException internalErrorException) {
            reserveVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, internalErrorException);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            reserveVo.setStatusCodeAndMessage(ResponseCodes.ACCOUNTDISABLED);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (VendorCurrencyNotSupportException | CurrencyNotSupportedException invalidCurrencyException) {
            reserveVo.setStatusCode(ResponseCodes.INVALIDCURRENCY);
            httpService.logError(httpRequestLog, invalidCurrencyException);

        } catch (AuthenticationException authenticationException) {
            reserveVo.setStatusCode(ResponseCodes.SESSIONEXPIRED);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            reserveVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);
            vendorService.setCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, reserveVo);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            reserveVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            reserveVo.setStatusCode(ResponseCodes.OK);
            reserveVo.setReal(betResultIdempotentViolationException.getBalance());
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if(invalidOperatorResponseException.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                reserveVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);
                vendorService.setCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, reserveVo);

            } else {
                reserveVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) {
            reserveVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            vendorService.buildResponseVo(reserveVo);
            httpService.end(httpRequestLog, reserveVo);

        }

        return reserveVo.getResponseXMLFormat();
    }

    private void doValidation(ReserveDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, ReserveDto reserveDto)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            AuthenticationException,
            InvalidPlayerException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            CredentialNotFoundException,
            InvalidRequestException {

        // Verify product group id
        vendorService.verifyProductId(gameSession.getVendorLineId(), reserveDto);

        // Verify vendor's access token
        vendorService.verifyAccessCode(gameSession.getVendorLineId(), reserveDto);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, reserveDto.getGameId());

        // Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, reserveDto.getExternalId());

        // Verify Username, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), reserveDto.getExternalId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), reserveDto.getCurrency(), CurrencyNotSupportedException::new);
    }

}
