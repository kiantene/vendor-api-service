package com.nextgen.gameaggregator.vendor.dblive.api.gamepayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.dblive.constant.TransferType.CANCEL;
import static com.nextgen.gameaggregator.vendor.dblive.constant.TransferType.REPAYOUT;

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
    private final ObjectMapper objectMapper;

    public GamePayoutAction(HttpService httpService, VendorLineService vendorLineService,
                            AgentPlayerService agentPlayerService, VendorGameService vendorGameService,
                            GameSessionService gameSessionService, WalletService walletService,
                            VendorService vendorService, WalletAdjustmentService walletAdjustmentService, ObjectMapper objectMapper) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletAdjustmentService = walletAdjustmentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = EndPoints.GAME_PAYOUT)
    public ResponseVo gamePayout(HttpServletRequest request) throws JsonProcessingException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        GamePayoutDataVo gamePayoutDataVo = new GamePayoutDataVo();
        GamePayoutParamDto gamePayoutParamDto = new GamePayoutParamDto();

        String md5Key = "";
        GameSession gameSession = new GameSession();
        try {

            String body = httpRequestLog.getRequestBody();

            //convert queryString to dto
            GamePayoutDto gamePayoutDto = HttpService.convertJsonToDto(body, GamePayoutDto.class);
            this.doValidation(gamePayoutDto);

            gamePayoutParamDto = VendorService.convertDto(gamePayoutDto.getParams(), GamePayoutParamDto.class);
            this.doValidation(gamePayoutParamDto);

            // using vendorPlayerId to find gameSession details
            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(gamePayoutParamDto.getLoginName());

            // Try to catch if session is expired and generate new session
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(vendorPlayerUsername, gamePayoutParamDto.getGameTypeId());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gamePayoutParamDto.getGameTypeId(), gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
                gameSessionService.updateByVendorGameCode(gameSession, gamePayoutParamDto.getGameTypeId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            //Verification
            doVerification(gamePayoutDto, gameSession, vendorPlayerUsername);
            md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            if (gamePayoutParamDto.getTransferType().equals(CANCEL) || gamePayoutParamDto.getTransferType().equals(REPAYOUT)) {

                AppendDto appendDto = VendorService.convertDto(gamePayoutDto.getParams(), AppendDto.class);

                BigDecimal appendBalance = walletAdjustmentService.processAdjustment(traceId, gameSession, appendDto, httpRequestLog);

                gamePayoutDataVo.setBalance(appendBalance.setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));
            } else {

                ResultType resultType = vendorService.calculateResultType(gamePayoutParamDto.getBetAmount(), gamePayoutParamDto.getWinAmount(), gamePayoutParamDto.getJackpotAmount(), false);

                BigDecimal balance = walletService.processBetResult(traceId, gameSession, gamePayoutParamDto, resultType, vendorService, httpRequestLog);

                gamePayoutDataVo.setBalance(balance.setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));
            }

            gamePayoutDataVo.setLoginName(gamePayoutParamDto.getLoginName());
            gamePayoutDataVo.setRealAmount(gamePayoutParamDto.getPayoutAmount());
            gamePayoutDataVo.setBadAmount(BigDecimal.ZERO);

            String signature = generateSignature(gamePayoutDataVo, md5Key);
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
            gamePayoutDataVo.setBalance(e.getBalance().setScale(Formats.BALANCE_SCALE, Formats.ROUNDING_MODE));

            String signature = generateSignature(gamePayoutDataVo, md5Key);
            responseVo.setResponseSuccess(gamePayoutDataVo, signature);
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
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    private <T> void doValidation(T requestObject) throws InvalidRequestException {
        // Validation with custom exception
        ValidationUtils.validateRequest(requestObject);
    }

    private String generateSignature(GamePayoutDataVo gamePayoutDataVo, String md5Key) throws JsonProcessingException {
        return VendorService.getMD5(objectMapper.writeValueAsString(gamePayoutDataVo) + md5Key);
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
