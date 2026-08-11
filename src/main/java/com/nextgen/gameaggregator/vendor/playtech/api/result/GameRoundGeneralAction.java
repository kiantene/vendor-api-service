package com.nextgen.gameaggregator.vendor.playtech.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playtech.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playtech.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playtech.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playtech.dto.GameRoundCloseDto;
import com.nextgen.gameaggregator.vendor.playtech.dto.PayDto;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonBalanceVo;
import com.nextgen.gameaggregator.vendor.playtech.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GameRoundGeneralAction {
    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;

    @Autowired
    public GameRoundGeneralAction(HttpService httpService, GameSessionService gameSessionService,
                                  AgentPlayerService agentPlayerService, VendorLineService vendorLineService,
                                  VendorService vendorService, WalletService walletService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.RESULT)
    public CommonGameRoundVo action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonGameRoundVo settleVo = new CommonGameRoundVo();
        BigDecimal finalBalance;
        CommonBalanceVo commonBalanceVo = new CommonBalanceVo();
        ResultType resultType;
        CommonGameRoundDto commonGameRoundDto = new CommonGameRoundDto();
        GameSession gameSession;
        try {

            String body = httpRequestLog.getRequestBody();
            commonGameRoundDto = HttpService.convertJsonToDto(body, CommonGameRoundDto.class);

            this.doValidation(commonGameRoundDto);

            String removedPrefix = vendorService.getExtractToken(commonGameRoundDto.getExternalToken());

            gameSession = gameSessionService.verifyToken(removedPrefix);

            String vendorGameCode = VendorService.resolveVendorGameCode(commonGameRoundDto.getGameCodeName(), commonGameRoundDto.getLiveTableDetails());
            if (!vendorGameCode.equals(gameSession.getVendorGameCode())) {
                vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(vendorGameCode, gameSession);
            }
            this.doVerification(commonGameRoundDto, gameSession);

            if (commonGameRoundDto.getPay() != null && "REFUND".equals(commonGameRoundDto.getPay().getType())) {
                finalBalance = walletService.processRollback(traceId, commonGameRoundDto, gameSession, vendorService,
                        httpRequestLog);
            } else {
                resultType = vendorService.calculateResultType(commonGameRoundDto.getBetAmount(), commonGameRoundDto.getWinAmount(), commonGameRoundDto.getJackpotAmount(), true);

                finalBalance = walletService.processBetResult(traceId, gameSession, commonGameRoundDto, resultType,
                        vendorService, httpRequestLog);
            }

            settleVo.setExternalTransactionCode(commonGameRoundDto.getExternalTransactionId());
            settleVo.setExternalTransactionDate(VendorService.convertBetOrSettleTime(commonGameRoundDto.getVendorSettleTime()));
            commonBalanceVo.setReal(finalBalance.setScale(2, RoundingMode.DOWN));
            commonBalanceVo.setTimestamp(VendorService.returnTime());
            settleVo.setBalance(commonBalanceVo);
        } catch (BetResultIdempotentViolationException e) {
            settleVo.setExternalTransactionCode(commonGameRoundDto.getExternalTransactionId());
            settleVo.setExternalTransactionDate(VendorService.convertBetOrSettleTime(commonGameRoundDto.getVendorSettleTime()));
            commonBalanceVo.setReal(e.getBalance().setScale(2, RoundingMode.DOWN));
            commonBalanceVo.setTimestamp(VendorService.returnTime());
            settleVo.setBalance(commonBalanceVo);
            httpService.logError(httpRequestLog, e);
        } catch (BetRefundIdempotentViolationException e) {
            settleVo.setExternalTransactionCode(commonGameRoundDto.getExternalTransactionId());
            settleVo.setExternalTransactionDate(VendorService.convertBetOrSettleTime(commonGameRoundDto.getVendorSettleTime()));
            commonBalanceVo.setReal(BigDecimal.ZERO);
            commonBalanceVo.setTimestamp(VendorService.returnTime());
            settleVo.setBalance(commonBalanceVo);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException | GameNotSupportedException e) {
            settleVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            settleVo.setError(ErrorVo.from(ResponseCodes.ERR_NO_BET));
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            settleVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            settleVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            settleVo.setError(ErrorVo.from(ResponseCodes.INTERNAL_ERROR));
            httpService.logError(httpRequestLog, e);

        } finally {
            settleVo.setRequestId(commonGameRoundDto.getRequestId());
            httpService.end(httpRequestLog, settleVo);
        }
        return settleVo;
    }

    private void doValidation(CommonGameRoundDto commonGameRoundDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonGameRoundDto);

        PayDto payDto = commonGameRoundDto.getPay();
        if (payDto != null) {
            ValidationUtils.validateRequest(payDto);
        }
        GameRoundCloseDto gameRoundCloseDto = commonGameRoundDto.getGameRoundClose();
        if (gameRoundCloseDto != null) {
            ValidationUtils.validateRequest(gameRoundCloseDto);
        }

    }

    private void doVerification(CommonGameRoundDto commonGameRoundDto, GameSession gameSession)
            throws InvalidPlayerException,
            CredentialNotFoundException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledVendorLineException {

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        String kioskPrefix = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KIOSK_PREFIX);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(kioskPrefix + "_" + gameSession.getVendorPlayerUsername(),
                commonGameRoundDto.getUserName(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

    }
}
