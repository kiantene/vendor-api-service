package com.nextgen.gameaggregator.vendor.dblive.api.betconfirm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.dblive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dblive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dblive.constant.ResponseCodes;
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
public class BetConfirmAction {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public BetConfirmAction(HttpService httpService, VendorLineService vendorLineService,
                            GameSessionService gameSessionService,
                            WalletService walletService, ValidationService validationService,
                            VendorService vendorService, RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.BET_CONFIRM)
    public ResponseVo betConfirm(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        GameSession gameSession = new GameSession();
        ResponseVo vo = new ResponseVo();
        BetParamsDto betParamsDto = new BetParamsDto();
        boolean isRequestExists = false;
        Integer languageId = 0;
        Integer platformId = 0;

        try {
            String body = httpRequestLog.getRequestBody();

            //convert queryString to dto
            BetConfirmDto betConfirmDto = HttpService.convertJsonToDto(body, BetConfirmDto.class);
            VendorService.doValidation(betConfirmDto);

            betParamsDto = VendorService.convertDto(betConfirmDto.getParams(), BetParamsDto.class);
            VendorService.doValidation(betParamsDto);

            if (requestIdempotentLogService.checkExists(betParamsDto, betParamsDto.getLoginName()) == null) {
                requestIdempotentLogService.create(betParamsDto, betParamsDto.getLoginName());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(betParamsDto.getLoginName());

            try {
                gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(vendorPlayerUsername);
                if (gameSession == null) throw new AuthenticationException();
                languageId = gameSession.getLanguageId();
                platformId = gameSession.getPlatformId();
                if (!gameSession.getVendorGameCode().equals(betParamsDto.getGameTypeId()))
                    throw new AuthenticationException();
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
                gameSessionService.updateByVendorGameCode(gameSession, betParamsDto.getGameTypeId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setLanguageId(languageId);
                gameSession.setPlatformId(platformId);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            //Verification
            doVerification(betConfirmDto, gameSession, vendorPlayerUsername);
            String md5Key = vendorLineService.getCredentialValueByName
                    (gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            // Process Result
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betParamsDto, body, httpRequestLog);

            BetConfirmDataVo betConfirmDataVo = new BetConfirmDataVo();
            BigDecimal balance = betEvent.getLastBalance();
            //Set Response Data
            betConfirmDataVo.setLoginName(betParamsDto.getLoginName());
            betConfirmDataVo.setRealBetAmount(betParamsDto.getBetTotalAmount());
            betConfirmDataVo.setRealBetInfo(betParamsDto.getBetInfo());
            betConfirmDataVo.setBalance(convertDecimal(balance));

            //MD5 betConfirmDataVo to signature
            String signature = VendorService.getMD5(betConfirmDataVo, md5Key);
            vo.setResponseSuccess(betConfirmDataVo, signature);

            if (vendorPlayerUsername.equals("1e905ywkv93b")) {
                Thread.sleep(4000); // 30 seconds in milliseconds
            }
        } catch (
                DisabledAgentPlayerException |
                DisabledGameException |
                InvalidRequestException |
                JsonProcessingException |
                JsonSyntaxException |
                DisabledVendorLineException e) {
            vo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            vo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException | InvalidPlayerException e) {
            vo.setResponseCode(ResponseCodes.INVALID_PLAYER_SESSION);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException exception) {
            vo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
            httpService.logError(httpRequestLog, exception);
        } catch (Exception e) {
            vo.setResponseCode(ResponseCodes.OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(betParamsDto, betParamsDto.getLoginName());
            }
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doVerification(BetConfirmDto betConfirmDto, GameSession gameSession, String vendorPlayerUsername) throws
            InvalidPlayerException, DisabledVendorLineException, CredentialNotFoundException,
            DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException, AuthenticationException {

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, vendorPlayerUsername);

        //Verify Signature is match
        String md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);
        VendorService.verifySignature(betConfirmDto.getParams(), md5Key, betConfirmDto.getSignature());
    }

}