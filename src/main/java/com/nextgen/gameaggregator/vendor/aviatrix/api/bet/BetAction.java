package com.nextgen.gameaggregator.vendor.aviatrix.api.bet;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aviatrix.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(EndPoints.PATH)
public class BetAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;

    @Autowired
    public BetAction(HttpService httpService,
                     GameSessionService gameSessionService,
                     WalletService walletService,
                     VendorLineService vendorLineService,
                     ValidationService validationService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
    }

    @PostMapping(EndPoints.BET)
    public ResponseEntity<ResponseVo> bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        try {
            String body = httpRequestLog.getRequestBody(); //extract request body
            BetDto dto = HttpService.convertJsonToDto(body, BetDto.class); //transfer to dto

            //validation
            this.doValidation(dto);

            //verify game session from token sent by vendor
            GameSession gameSession = gameSessionService.verifyToken(dto.getSessionToken());

            //verify body values
            this.doVerification(dto, gameSession);

            //process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);

            //mapping
            responseVo.setBalance(betEvent.getLastBalance().setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).toBigInteger());
            responseVo.setCreatedAt(VendorService.returnTime());

        } catch (AuthenticationException authenticationException) {
            responseVo.setMessage(ResponseCodes.INVALID_SESSION_TOKEN);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, authenticationException);
        } catch (NullPointerException |
                 InvalidFormatException |
                 InvalidRequestException invalidRequestException) {
            responseVo.setMessage(ResponseCodes.INVALID_REQUEST);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setMessage(ResponseCodes.INVALID_TRANSACTION);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, transactionStillProcessingException);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).toBigInteger());
            responseVo.setCreatedAt(VendorService.returnTime());
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setMessage(ResponseCodes.INVALID_PLAYER_CURRENCY);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, currencyNotSupportedException);
        } catch (TokenExpiredException tokenExpiredException) {
            responseVo.setMessage(ResponseCodes.SESSION_TOKEN_EXPIRED);
            responseVo.setHttpStatus(HttpStatus.UNAUTHORIZED);
            httpService.logError(httpRequestLog, tokenExpiredException);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setMessage(ResponseCodes.INSUFICIENT_BALANCE);
            responseVo.setHttpStatus(HttpStatus.FORBIDDEN);
            httpService.logError(httpRequestLog, insufficientBalanceException);
        } catch (InvalidVendorLineException | DisabledVendorLineException invalidVendorLineException) {
            responseVo.setMessage(ResponseCodes.PLATFORM_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setMessage(ResponseCodes.PLAYER_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);
        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setMessage(ResponseCodes.PRODUCT_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, gameNotSupportedException);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setMessage(ResponseCodes.PLAYER_BANNED);
            responseVo.setHttpStatus(HttpStatus.FORBIDDEN);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
        } catch (Exception e) {
            responseVo.setMessage(ResponseCodes.UNKNOWN_ERROR);
            responseVo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, responseVo.getHttpStatus());
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        //basic validations
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, GameNotSupportedException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException {
        //verify channel id from vendor
        String cid = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CID);
        ValidationUtils.isEquals(cid, dto.getCid(), InvalidVendorLineException::new);

        //bet eligibility check
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getProductId(), GameNotSupportedException::new);
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        //check player id is same as session id
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
    }
}
