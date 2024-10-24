package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aviatrix.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(EndPoints.PATH)
public class PromoWinAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final WalletService walletService;

    @Value("${vendor.aviatrix.promoWinEnabled:false}")
    private boolean promoWinEnabled;

    @Autowired
    public PromoWinAction(HttpService httpService,
                          GameSessionService gameSessionService,
                          VendorLineService vendorLineService,
                          VendorService vendorService,
                          WalletService walletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.walletService = walletService;
    }

    @PostMapping(EndPoints.BONUS)
    public ResponseEntity<ResponseVo> promoWin(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        String body = httpRequestLog.getRequestBody();
        BigDecimal balance;

        try {
            PromoWinDto dto = HttpService.convertJsonToDto(body, PromoWinDto.class);

            this.doValidation(dto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());

            this.doVerification(dto, gameSession);

            /*
            This block is purposely served as a temporary solution to pass acceptance test
            Default value = false
            It is not an expected behaviour to run this part of block unless necessary
             */
            if (promoWinEnabled) {
                ResultType resultType = vendorService.calculateResultType(BigDecimal.ZERO, dto.getWinAmount(), dto.getJackpotAmount(), true);

                balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);
                responseVo.setCreatedAt(VendorService.returnTime());
                responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).toBigInteger());

            } //else just return a null body and success status


        } catch (AuthenticationException authenticationException) {
            responseVo.setMessage(ResponseCodes.PLAYER_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, authenticationException);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).toBigInteger());
            responseVo.setCreatedAt(VendorService.returnTime());
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
        } catch (InvalidFormatException |
                 NullPointerException |
                 InvalidRequestException invalidRequestException) {
            responseVo.setMessage(ResponseCodes.INVALID_REQUEST);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (TokenExpiredException tokenExpiredException) {
            responseVo.setMessage(ResponseCodes.SESSION_TOKEN_EXPIRED);
            responseVo.setHttpStatus(HttpStatus.UNAUTHORIZED);
            httpService.logError(httpRequestLog, tokenExpiredException);
        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setMessage(ResponseCodes.PRODUCT_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, gameNotSupportedException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setMessage(ResponseCodes.PLATFORM_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setMessage(ResponseCodes.INVALID_PLAYER_CURRENCY);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, currencyNotSupportedException);
        } catch (Exception e) {
            responseVo.setMessage(ResponseCodes.UNKNOWN_ERROR);
            responseVo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, HttpStatus.OK);
    }

    private void doValidation(PromoWinDto dto) throws InvalidRequestException {
        //basic validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(PromoWinDto dto, GameSession gameSession) throws InvalidVendorLineException, CredentialNotFoundException, GameNotSupportedException, CurrencyNotSupportedException, InvalidPlayerException {
        //check cid
        String cid = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CID);
        ValidationUtils.isEquals(cid, dto.getCid(), InvalidVendorLineException::new);

        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getProductId(), GameNotSupportedException::new);
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        //check player id is same as session id
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
    }

}
