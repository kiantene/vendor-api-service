package com.nextgen.gameaggregator.vendor.ygg.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ygg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ygg.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.ygg.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class WagerAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private SettledBetService settledBetService;

    @Autowired
    public WagerAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService,
                       VendorLineService vendorLineService, AgentPlayerService agentPlayerService,
                       VendorGameService vendorGameService, VendorService vendorService,
                       ValidationService validationService, SettledBetService settledBetService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.settledBetService = settledBetService;
    }

    @GetMapping(path = EndPoints.BET)
    public WagerVo wager(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        WagerDto dto = new WagerDto();

        WagerVo responseVo = new WagerVo();

        DataVo data = new DataVo();
        try {
            // Log Request Body
            httpRequestLog.setRequestBody(request.getQueryString());

            // Convert query string to DTO
            dto = HttpService.convertQueryStringToDto(request.getQueryString(), WagerDto.class);

            // Do validation Request
            ValidationUtils.validateRequest(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getSessionToken());

            // Do verification
            doVerification(dto, gameSession);

            //Check Settle Bet Exist
            verifySettledBet(dto, gameSession);
            
            // Process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession,
                    dto, httpRequestLog.getRequestBody(), httpRequestLog);

            // Get balance
            BigDecimal balance = betEvent.getLastBalance();

            // Set response body
            data.setCurrency(dto.getCurrency());
            data.setApplicableBonus(BigDecimal.valueOf(0));
            data.setHomeCurrency(dto.getCurrency());
            data.setOrganization(dto.getOrg());
            data.setBalance(balance);
            data.setNickName(gameSession.getVendorPlayerUsername());
            data.setPlayerId(dto.getPlayerId());

            // Set response code and Vodata
            responseVo.setData(data);
            responseVo.setCode(ResponseCode.SUCCESS.code);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);

            // Return original result when idempotent
            data.setCurrency(dto.getCurrency());
            data.setApplicableBonus(BigDecimal.valueOf(0));
            data.setHomeCurrency(dto.getCurrency());
            data.setOrganization(dto.getOrg());
            data.setBalance(e.getBalance());
            data.setNickName(dto.getPlayerId());
            data.setPlayerId(dto.getPlayerId());

            // Set response code and Vodata
            responseVo.setCode(ResponseCode.SUCCESS.code);
            responseVo.setData(data);
        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_AUTHORIZED);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_LOGGED_IN);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_OVERDRAFT);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doVerification(WagerDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException,
            AuthenticationException, IllegalArgumentException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // Verify currency
        vendorService.verifyCurrency(gameSession.getVendorCurrencyCode(), dto.getCurrency());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

    }

    private void verifySettledBet(
            WagerDto dto, GameSession gameSession) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getRoundId());

        if (!settledBetList.isEmpty() && settledBetList.get(0).getOperatorStatus().equals(1)) {
            throw new BetResultIdempotentViolationException(settledBetList.get(0));
        }
    }
}
