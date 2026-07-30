package com.nextgen.gameaggregator.vendor.koolbet.api.betnsettle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.config.KoolbetConfig;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.service.VendorService;
import com.nextgen.gameaggregator.vendor.koolbet.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetNSettleAction {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorService vendorService;

    private final ValidationService validationService;

    private final RequestIdempotentLogService requestIdempotentLogService;

    private final MigrationRoundDataService migrationRoundDataService;

    @Autowired
    public BetNSettleAction(HttpService httpService, GameSessionService gameSessionService,
                            WalletService walletService,
                            VendorService vendorService,
                            ValidationService validationService,
                            RequestIdempotentLogService requestIdempotentLogService,
                            MigrationRoundDataService migrationRoundDataService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.migrationRoundDataService = migrationRoundDataService;
    }

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();
        GameSession gameSession = new GameSession();
        CommonVo responseVo = new CommonVo();
        boolean isRequestExists = false;
        BetNSettleDto betNSettleDto = new BetNSettleDto();
        String vendorPlayerUsername = null;
        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            betNSettleDto = HttpService.convertJsonToDto(body, BetNSettleDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(betNSettleDto);

            //get rawGameSession by token id
            gameSession = gameSessionService.verifyToken(betNSettleDto.getToken());
            vendorPlayerUsername = gameSession.getVendorPlayerUsername();

            if (requestIdempotentLogService.checkExists(betNSettleDto, vendorPlayerUsername) == null) {
                requestIdempotentLogService.create(betNSettleDto, vendorPlayerUsername);
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(String.valueOf(betNSettleDto.getGame()), gameSession);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession);

            // Write-ahead v1 round marker (OVI-2153): the request is authenticated and verified
            // here, but the wallet op has not run yet. Recording intent before processBetResult
            // pins the round to v1 even if processing later throws after persisting state,
            // closing the persist-then-throw split-brain window. markAsV1 swallows its own
            // failures, so it never breaks the v1 callback.
            migrationRoundDataService.markAsV1(KoolbetConfig.CLASS_NAME, betNSettleDto.getRoundId());

            //make a ResultType for bet and settle process indicator
            ResultType resultType = vendorService.calculateResultType(betNSettleDto.getBetAmount(),
                    betNSettleDto.getWinAmount(), betNSettleDto.getJackpotAmount(), true);
            //Process full bet data
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betNSettleDto, resultType,
                    vendorService, httpRequestLog);

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(balance);

        } catch (BetResultIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.BET_ALREADY_ACCEPTED);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(e.getBalance());
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {
            if (e.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            } else {
                responseVo.setResponseCode(ResponseCode.OTHER_ERROR);
            }
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {

            if (!isRequestExists) {
                requestIdempotentLogService.delete(betNSettleDto, vendorPlayerUsername);
            }
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetNSettleDto betNSettleDto, GameSession gameSession) throws
            AuthenticationException, CurrencyNotSupportedException, InvalidPlayerException, DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betNSettleDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(betNSettleDto.getGame()), GameNotSupportedException::new);
        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
    }
}
