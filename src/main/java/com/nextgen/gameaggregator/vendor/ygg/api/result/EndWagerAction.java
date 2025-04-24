package com.nextgen.gameaggregator.vendor.ygg.api.result;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
public class EndWagerAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private SettledBetService settledBetService;

    @Autowired
    public EndWagerAction(HttpService httpService, GameSessionService gameSessionService,
                          VendorLineService vendorLineService, AgentPlayerService agentPlayerService,
                          VendorGameService vendorGameService, WalletService walletService, VendorService vendorService
            , SettledBetService settledBetService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.settledBetService = settledBetService;

    }

    @GetMapping(path = EndPoints.SETTLED)
    public EndWagerVo endWager(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        EndWagerDto dto = new EndWagerDto();

        EndWagerVo responseVo = new EndWagerVo();

        DataVo data = new DataVo();
        try {
            // Log Request Body
            httpRequestLog.setRequestBody(request.getQueryString());

            // Convert query string to DTO
            dto = HttpService.convertQueryStringToDto(request.getQueryString(), EndWagerDto.class);

            // Do validation
            ValidationUtils.validateRequest(dto);

            GameSession gameSession = new GameSession();
            try {
                // Get Session by playerId
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
                // Do verification
                doVerification(dto, gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getPlayerId()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, dto.getCat5());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // Do verification
            doVerification(dto, gameSession);

            verifySettledBet(dto, gameSession);

            // Get Result and calculate balance based on ResultType
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(),
                    dto.getWinAmount(), dto.getJackpotAmount(), true);

            // Process and get new balance
            BigDecimal balance = walletService.processBetResult(traceId, gameSession,
                    dto, resultType, vendorService, httpRequestLog);

            // Set response data

            data.setCurrency(dto.getCurrency());
            data.setApplicableBonus(BigDecimal.valueOf(0));
            data.setHomeCurrency(dto.getCurrency());
            data.setOrganization(dto.getOrg());
            data.setBalance(balance);
            data.setNickName(gameSession.getVendorPlayerUsername());
            data.setPlayerId(String.valueOf(dto.getPlayerId()));

            // Set response code and data
            responseVo.setData(data);
            responseVo.setCode(ResponseCode.SUCCESS.code);

        } catch (BetResultIdempotentViolationException e) {
            // Return original result when idempotent
            data.setCurrency(dto.getCurrency());
            data.setApplicableBonus(BigDecimal.valueOf(0));
            data.setHomeCurrency(dto.getCurrency());
            data.setOrganization(dto.getOrg());
            data.setBalance(e.getBalance());
            data.setNickName(dto.getPlayerId());
            data.setPlayerId(String.valueOf(dto.getPlayerId()));


            // Set response code and Vodata
            responseVo.setCode(ResponseCode.SUCCESS.code);
            responseVo.setData(data);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_AUTHORIZED);
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

    private void doVerification(EndWagerDto dto, GameSession gameSession) throws InvalidPlayerException {

        // Verify currency
        vendorService.verifyCurrency(gameSession.getVendorCurrencyCode(), dto.getCurrency());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

    }

    private void verifySettledBet(EndWagerDto dto, GameSession gameSession) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getRoundId());

        if (!settledBetList.isEmpty() && settledBetList.get(0).getOperatorStatus().equals(1)) {
            throw new BetResultIdempotentViolationException(settledBetList.get(0));
        }
    }
}
