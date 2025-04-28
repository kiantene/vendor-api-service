package com.nextgen.gameaggregator.vendor.dblive.api.activitypayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.dblive.constant.*;
import com.nextgen.gameaggregator.vendor.dblive.service.VendorService;
import com.nextgen.gameaggregator.vendor.dblive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.dblive.service.VendorService.convertDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class ActivityPayoutAction {
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final ValidationService validationService;
    private final WalletService walletService;

    public ActivityPayoutAction(HttpService httpService, VendorLineService vendorLineService,
                                VendorService vendorService, GameSessionService gameSessionService,
                                ValidationService validationService, WalletService walletService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.validationService = validationService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.ACTIVITY_PAYOUT)
    public ResponseVo gamePayout(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        ActivityPayoutCommonDto commonDto = null;
        String md5Key = "";
        try {
            String requestBody = httpRequestLog.getRequestBody();

            ActivityPayoutDto activityPayoutDto = HttpService.convertJsonToDto(requestBody, ActivityPayoutDto.class);
            VendorService.doValidation(activityPayoutDto);

            commonDto = VendorService.convertDto(activityPayoutDto.getParams(), ActivityPayoutCommonDto.class);
            VendorService.doValidation(commonDto);

            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(commonDto.getLoginName());
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayerUsername);

            doVerification(activityPayoutDto, gameSession, vendorPlayerUsername);
            md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            ActivityPayoutDataVo payoutDataVo = processPayoutByType(commonDto, gameSession, traceId, httpRequestLog);

            String signature = VendorService.getMD5(payoutDataVo, md5Key);
            responseVo.setResponseSuccess(payoutDataVo, signature);
        } catch (BetNotFoundException e) {
            responseVo.setResponseCode(ResponseCodes.BET_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 TransactionStillProcessingException |
                 JsonProcessingException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            ActivityPayoutDataVo payoutDataVo = new ActivityPayoutDataVo();
            payoutDataVo.setBalance(convertDecimal(e.getBalance()));
            payoutDataVo.setLoginName(commonDto.getLoginName());
            payoutDataVo.setBadAmount(ZERO_AMOUNT);
            payoutDataVo.setRealAmount(commonDto.getPayoutAmount());
            String signature = "";

            try {
                signature = VendorService.getMD5(payoutDataVo, md5Key);
                responseVo.setResponseSuccess(payoutDataVo, signature);
            } catch (JsonProcessingException ex) {
                responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            }


            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException | InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_PLAYER_SESSION);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCodes.OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private ActivityPayoutDataVo processPayoutByType(ActivityPayoutCommonDto commonDto, GameSession gameSession,
                                                     String traceId, HttpRequestLog httpRequestLog) throws Exception {

        ActivityPayoutDataVo payoutDataVo = new ActivityPayoutDataVo();
        payoutDataVo.setLoginName(commonDto.getLoginName());
        payoutDataVo.setBadAmount(ZERO_AMOUNT);
        payoutDataVo.setRealAmount(commonDto.getPayoutAmount());

        if (isPayoutOrDeduction(commonDto.getPayoutType())) {
            processPayoutOrDeduction(commonDto, gameSession, traceId, httpRequestLog, payoutDataVo);
        } else if (TransferType.ROLLBACK.equals(commonDto.getPayoutType())) {
            processRollback(commonDto, gameSession, httpRequestLog, payoutDataVo);
        }

        return payoutDataVo;
    }

    private boolean isPayoutOrDeduction(String payoutType) {
        return TransferType.DEDUCTION.equals(payoutType) || TransferType.PAYOUT.equals(payoutType);
    }

    private void processPayoutOrDeduction(ActivityPayoutCommonDto commonDto, GameSession gameSession,
                                          String traceId, HttpRequestLog httpRequestLog,
                                          ActivityPayoutDataVo payoutDataVo) throws Exception {
        ActivityPayoutParamsDto paramsDto = VendorService.convertDto(commonDto, ActivityPayoutParamsDto.class);

        ResultType resultType = vendorService.calculateResultType
                (paramsDto.getBetAmount(), paramsDto.getWinAmount(), paramsDto.getJackpotAmount(), true);

        BigDecimal balance = walletService.processBetResult
                (traceId, gameSession, paramsDto, resultType, vendorService, httpRequestLog);

        payoutDataVo.setBalance(balance.setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));
    }

    private void processRollback(ActivityPayoutCommonDto commonDto, GameSession gameSession,
                                 HttpRequestLog httpRequestLog, ActivityPayoutDataVo payoutDataVo) throws Exception {

        ActivityPayoutRollbackDto rollbackDto = VendorService.convertDto
                (commonDto, ActivityPayoutRollbackDto.class);

        WalletRequest walletRequest = walletService.processRollback
                (rollbackDto, gameSession, vendorService, httpRequestLog);

        payoutDataVo.setBalance(walletRequest.getBalanceAfter().setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));
    }

    private void doVerification(ActivityPayoutDto activityPayoutDto, GameSession gameSession, String vendorPlayerUsername) throws
            InvalidPlayerException, DisabledVendorLineException, CredentialNotFoundException,
            DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException, AuthenticationException {

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, vendorPlayerUsername);

        // Uncomment the following line to enable signature verification
        String md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);
        VendorService.verifySignature(activityPayoutDto.getParams(), md5Key, activityPayoutDto.getSignature());
    }
}