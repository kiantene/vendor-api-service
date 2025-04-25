package com.nextgen.gameaggregator.vendor.dblive.api.betcancel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dblive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dblive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dblive.constant.Formats;
import com.nextgen.gameaggregator.vendor.dblive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dblive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.dblive.service.VendorService;
import com.nextgen.gameaggregator.vendor.dblive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetCancelAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorLineService vendorLineService;
    private final ObjectMapper objectMapper;

    public BetCancelAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService,
                           VendorService vendorService, AgentPlayerService agentPlayerService, VendorGameService vendorGameService, VendorLineService vendorLineService, ObjectMapper objectMapper) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorLineService = vendorLineService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(EndPoints.BET_CANCEL)
    public ResponseVo betCancel(HttpServletRequest request) throws JsonProcessingException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        BetCancelDataVo betCancelDataVo = new BetCancelDataVo();

        BetCancelParamsDto betCancelParamsDto = new BetCancelParamsDto();

        String md5Key = "";

        GameSession gameSession = new GameSession();
        try {
            String body = httpRequestLog.getRequestBody();

            //convert queryString to dto validate request param
            BetCancelDto betCancelDto = HttpService.convertJsonToDto(body, BetCancelDto.class);
            this.doValidation(betCancelDto);

            betCancelParamsDto = VendorService.convertDto(betCancelDto.getParams(), BetCancelParamsDto.class);
            this.doValidation(betCancelParamsDto);

            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(betCancelParamsDto.getLoginName());

            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(vendorPlayerUsername, betCancelParamsDto.getGameTypeId());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betCancelParamsDto.getGameTypeId(), gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
                gameSessionService.updateByVendorGameCode(gameSession, betCancelParamsDto.getGameTypeId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            doVerification(betCancelDto, gameSession, vendorPlayerUsername);
            md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            // Retrieve the latest wallet balance from Operator
            WalletRequest walletRequest = walletService.processRollback(betCancelParamsDto, gameSession, vendorService, httpRequestLog);

            betCancelDataVo.setBalance(walletRequest.getBalanceAfter().setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));
            betCancelDataVo.setRollbackAmount(walletRequest.getBetAmount());
            betCancelDataVo.setLoginName(betCancelParamsDto.getLoginName());

            String signature = generateSignature(betCancelDataVo, md5Key);
            responseVo.setResponseSuccess(betCancelDataVo, signature);
        } catch (InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 JsonSyntaxException |
                 TransactionStillProcessingException |
                 JsonProcessingException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            betCancelDataVo.setRollbackAmount(e.getBetInformation().getBetAmount());
            betCancelDataVo.setLoginName(betCancelParamsDto.getLoginName());
            betCancelDataVo.setBalance(e.getBalance().setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));

            String signature = generateSignature(betCancelDataVo, md5Key);
            responseVo.setResponseSuccess(betCancelDataVo, signature);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
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

    private <T> void doValidation(T requestObject) throws InvalidRequestException {
        // Validation with custom exception
        ValidationUtils.validateRequest(requestObject);
    }

    private String generateSignature(BetCancelDataVo betCancelDataVo, String md5Key) throws JsonProcessingException {
        return VendorService.getMD5(objectMapper.writeValueAsString(betCancelDataVo) + md5Key);
    }

    private void doVerification(CommonDto gamePayoutDto, GameSession gameSession, String vendorPlayerUsername) throws
            InvalidPlayerException, DisabledVendorLineException, CredentialNotFoundException,
            DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        //Verify Signature is match
        String md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);
        VendorService.verifySignature(gamePayoutDto.getParams(), md5Key, gamePayoutDto.getSignature());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), vendorPlayerUsername, InvalidPlayerException::new);
    }
}
