package com.nextgen.gameaggregator.vendor.dblive.api.gamepayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dblive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dblive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dblive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dblive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.dblive.service.VendorService;
import com.nextgen.gameaggregator.vendor.dblive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.dblive.constant.TransferType.CANCEL;
import static com.nextgen.gameaggregator.vendor.dblive.constant.TransferType.REPAYOUT;
import static com.nextgen.gameaggregator.vendor.dblive.service.VendorService.convertDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GamePayoutAction {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final WalletAdjustmentService walletAdjustmentService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public GamePayoutAction(HttpService httpService, VendorLineService vendorLineService,
                            AgentPlayerService agentPlayerService, VendorGameService vendorGameService,
                            GameSessionService gameSessionService, WalletService walletService,
                            VendorService vendorService, WalletAdjustmentService walletAdjustmentService, RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletAdjustmentService = walletAdjustmentService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.GAME_PAYOUT)
    public ResponseVo gamePayout(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        GamePayoutDataVo gamePayoutDataVo = new GamePayoutDataVo();
        GamePayoutParamDto gamePayoutParamDto = new GamePayoutParamDto();
        boolean isRequestExists = false;
        Integer languageId = 0;
        Integer platformId = 0;

        String md5Key = "";
        GameSession gameSession = new GameSession();
        try {

            String body = httpRequestLog.getRequestBody();

            //convert queryString to dto
            GamePayoutDto gamePayoutDto = HttpService.convertJsonToDto(body, GamePayoutDto.class);
            VendorService.doValidation(gamePayoutDto);

            gamePayoutParamDto = VendorService.convertDto(gamePayoutDto.getParams(), GamePayoutParamDto.class);
            VendorService.doValidation(gamePayoutParamDto);

            // 3. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(gamePayoutParamDto, gamePayoutParamDto.getLoginName()) == null) {
                requestIdempotentLogService.create(gamePayoutParamDto, gamePayoutParamDto.getLoginName());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // using vendorPlayerId to find gameSession details
            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(gamePayoutParamDto.getLoginName());

            // Try to catch if session is expired and generate new session
            try {
                gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(vendorPlayerUsername);
                languageId = gameSession.getLanguageId();
                platformId = gameSession.getPlatformId();
                if (!gameSession.getVendorGameCode().equals(gamePayoutParamDto.getGameTypeId()))
                    throw new AuthenticationException();
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
                gameSessionService.updateByVendorGameCode(gameSession, gamePayoutParamDto.getGameTypeId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setLanguageId(languageId);
                gameSession.setPlatformId(platformId);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            //Verification
            doVerification(gamePayoutDto, gameSession, vendorPlayerUsername);
            md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            if (gamePayoutParamDto.getTransferType().equals(CANCEL) || gamePayoutParamDto.getTransferType().equals(REPAYOUT)) {

                AppendDto appendDto = VendorService.convertDto(gamePayoutDto.getParams(), AppendDto.class);

                BigDecimal appendBalance = walletAdjustmentService.processAdjustment(traceId, gameSession, appendDto, httpRequestLog);

                gamePayoutDataVo.setBalance(convertDecimal(appendBalance));
            } else {

                ResultType resultType = vendorService.calculateResultType(gamePayoutParamDto.getBetAmount(), gamePayoutParamDto.getWinAmount(), gamePayoutParamDto.getJackpotAmount(), false);

                BigDecimal balance = walletService.processBetResult(traceId, gameSession, gamePayoutParamDto, resultType, vendorService, httpRequestLog);

                gamePayoutDataVo.setBalance(convertDecimal(balance));
            }

            gamePayoutDataVo.setLoginName(gamePayoutParamDto.getLoginName());
            gamePayoutDataVo.setRealAmount(gamePayoutParamDto.getPayoutAmount());
            gamePayoutDataVo.setBadAmount(BigDecimal.ZERO);

            String signature = VendorService.getMD5(gamePayoutDataVo, md5Key);
            responseVo.setResponseSuccess(gamePayoutDataVo, signature);
        } catch (InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException |
                 JsonSyntaxException |
                 InvalidRequestException |
                 JsonProcessingException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            gamePayoutDataVo.setLoginName(gamePayoutParamDto.getLoginName());
            gamePayoutDataVo.setRealAmount(gamePayoutParamDto.getPayoutAmount());
            gamePayoutDataVo.setBadAmount(BigDecimal.ZERO);
            gamePayoutDataVo.setBalance(convertDecimal(e.getBalance()));
            String signature = "";

            try {
                signature = VendorService.getMD5(gamePayoutDataVo, md5Key);
                responseVo.setResponseSuccess(gamePayoutDataVo, signature);
            } catch (JsonProcessingException ex) {
                responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            }

            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_PLAYER_SESSION);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCodes.OTHER_ERROR);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(gamePayoutParamDto, gamePayoutParamDto.getLoginName());
            }
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

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
