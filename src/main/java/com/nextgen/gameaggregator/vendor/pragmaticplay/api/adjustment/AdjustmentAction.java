package com.nextgen.gameaggregator.vendor.pragmaticplay.api.adjustment;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.RawBetAdjustmentLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public class AdjustmentAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletAdjustmentService walletAdjustmentService;

    @PostMapping(path = Endpoints.ADJUSTMENT)
    public ResponseVo adjustment(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        AdjustmentVo responseVo = new AdjustmentVo();
        String traceId = httpRequestLog.getId();
        String vendorCurrencyCode = "";

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            AdjustmentDto dto = HttpService.convertQueryStringToDto(body, AdjustmentDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Retrieve and verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Pass data to wallet service process adjustment
            BigDecimal balance = walletAdjustmentService.processAdjustment(traceId, gameSession, dto, httpRequestLog);

            responseVo.setTransactionId(VendorService.getTransactionId(traceId));
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (InvalidSignatureException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);

        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);

        } catch (InvalidRequestException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (CredentialNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (BetNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);

        } catch (BetAdjustmentIdempotentViolationException e) {
            RawBetAdjustmentLog rawBetAdjustmentLog = e.getRawBetAdjustmentLog();
            responseVo.setTransactionId(VendorService.getTransactionId(rawBetAdjustmentLog.getBetAdjustmentId()));
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setCash(rawBetAdjustmentLog.getBalance());
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (SettledBetNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);

        } catch (TransactionStillProcessingException e) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);

        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidAgentApiCredentialException e) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);

        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        return responseVo;
    }

    private void doValidation(AdjustmentDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);

        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
        if (dto.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(HttpRequestLog request, AdjustmentDto dto, GameSession gameSession) throws AuthenticationException, CredentialNotFoundException, InvalidSignatureException {

        // 1. Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        if (!gameSession.getVendorGameCode().equals("101")) {
            ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);
        }

        // 4. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 5. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
