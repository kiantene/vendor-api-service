package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ReleaseAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.RELEASE)
    public String release(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ReleaseVo releaseVo = new ReleaseVo();
        XmlMapper xmlMapper = new XmlMapper();
        String releaseVoXml = "";

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            ReleaseDto releaseDto = xmlMapper.readValue(body, ReleaseDto.class);
            log.info("Playngo Release body: " + body);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(releaseDto);

            // Get game session or verify Token
            GameSession gameSession = vendorService.getGameSession(releaseDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, releaseDto);

            // Process Bet Result
            ResultType resultType = vendorService.calculateResultType(releaseDto.getBetAmount(), releaseDto.getWinAmount(), releaseDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, releaseDto, resultType, vendorService, httpRequestLog);

            // Construct VO
            releaseVo.setStatusCode(ResponseCodes.OK);
            releaseVo.setReal(balance);

        } catch (InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 GameNotSupportedException |
                 CredentialNotFoundException |
                 JsonProcessingException |
                 InvalidRequestException |
                 NoSuchMethodException |
                 InvocationTargetException |
                 IllegalAccessException internalErrorException) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (VendorCurrencyNotSupportException | CurrencyNotSupportedException invalidCurrencyException) {
            releaseVo.setStatusCode(ResponseCodes.INVALIDCURRENCY);

        } catch (AuthenticationException authenticationException) {
            releaseVo.setStatusCode(ResponseCodes.SESSIONEXPIRED);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            releaseVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            releaseVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (BetNotFoundException betNotFoundException) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if(invalidOperatorResponseException.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                releaseVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);

            } else {
                releaseVo.setStatusCode(ResponseCodes.INTERNAL);
                httpService.logError(httpRequestLog, invalidOperatorResponseException);

            }

        } catch (Exception exception) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            try {
                releaseVoXml = xmlMapper.writeValueAsString(releaseVo);

            } catch (JsonProcessingException e) {
                releaseVo.setStatusCode(ResponseCodes.INTERNAL);

            }

            releaseVo.setResponseXMLFormat(releaseVoXml);
            httpService.end(httpRequestLog, releaseVo);

        }

        return releaseVoXml;
    }

    private void doValidation(ReleaseDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, ReleaseDto releaseDto)
            throws
            InvalidPlayerException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            AuthenticationException,
            CredentialNotFoundException {

        // Verify product group id
        vendorService.verifyProductId(gameSession.getVendorLineId(), releaseDto);

        // Verify vendor's access token
        vendorService.verifyAccessCode(gameSession.getVendorLineId(), releaseDto);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, releaseDto.getGameId());

        // Verify Username, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), releaseDto.getExternalId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), releaseDto.getCurrency(), CurrencyNotSupportedException::new);

    }

}
