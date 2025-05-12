package com.nextgen.gameaggregator.vendor.avatarux.api.betnsettle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Headers;
import com.nextgen.gameaggregator.vendor.avatarux.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import com.nextgen.gameaggregator.vendor.avatarux.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetNSettleAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final UnsettledBetCachingService unsettledBetCachingService;
    private final SettledBetService settledBetService;

    public BetNSettleAction(WalletService walletService,
                            HttpService httpService,
                            ValidationService validationService,
                            VendorService vendorService,
                            VendorLineService vendorLineService,
                            GameSessionService gameSessionService,
                            UnsettledBetCachingService unsettledBetCachingService,
                            SettledBetService settledBetService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.settledBetService = settledBetService;
    }

    @PutMapping(path = EndPoints.TRANSACTION)
    public BetNSettleVo betNSettleAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String serverAuthorization = request.getHeader(Headers.SERVER_AUTHORIZATION);
        String authorization = request.getHeader(Headers.AUTHORIZATION);
        //Add request header log
        httpRequestLog.setRequestBody("Request Body: \n" + httpRequestLog.getRequestBody() + "\nRequest Header: \n" + vendorService.getHeaders(request));
        BetNSettleVo betNSettleVo = new BetNSettleVo();
        BetNSettleDto betNSettleDto;
        BigDecimal balance;

        try {
            betNSettleDto = HttpService.convertJsonToDto(body, BetNSettleDto.class);
            betNSettleDto.setXServerAuthorization(serverAuthorization);
            betNSettleDto.setAuthorization(authorization);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betNSettleDto);

            // Get GameSession with username
            GameSession gameSession = gameSessionService.verifyToken(authorization.substring(7));
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betNSettleDto.getGame(), gameSession);

            // Verify parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession, body);

            switch (betNSettleDto.getType()) {
                case "withdraw":
                    //Bet
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betNSettleDto, body, httpRequestLog);
                    balance = betEvent.getLastBalance();
                    betNSettleVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
                    break;

                case "deposit":
                    //Settle
                    //Check for Bet
                    unsettledBetCachingService.getUnsettledBetByRoundId(betNSettleDto.getVendorBetId(), betNSettleDto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

                    settleBet(betNSettleDto, gameSession, traceId, betNSettleVo, httpRequestLog);
                    break;

                default:
                    throw new InvalidRequestException();
            }

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            betNSettleVo.setBalance(e.getBalance().setScale(2, RoundingMode.DOWN));
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            betNSettleVo.setError(new ErrorVo());
            betNSettleVo.getError().setCode(ResponseCode.INSUFFICIENT_FUNDS.code);
            betNSettleVo.getError().setMessage(ResponseCode.INSUFFICIENT_FUNDS.description);
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            betNSettleVo.setError(new ErrorVo());
            betNSettleVo.getError().setCode(ResponseCode.SERVER_UNAUTHORIZED.code);
            betNSettleVo.getError().setMessage(ResponseCode.SERVER_UNAUTHORIZED.description);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            betNSettleVo.setError(new ErrorVo());
            betNSettleVo.getError().setCode(ResponseCode.UNKNOWN.code);
            betNSettleVo.getError().setMessage(ResponseCode.UNKNOWN.description);
        } finally {
            httpService.end(httpRequestLog, betNSettleVo);
        }
        return betNSettleVo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetNSettleDto dto, GameSession gameSession, String body) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException {
        //1. validate vendor username, agent vendor line, player status, and game status
        if (dto.getType().equals("withdraw")) {
            validationService.validateEligibleBet(gameSession, dto.getNativeId());
        }

        //2. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getNativeId(), AuthenticationException::new);

        //3. Verify Authorization
        String authorizationToken = dto.getAuthorization();
        if (authorizationToken == null || !authorizationToken.startsWith("Bearer ")) {
            throw new AuthenticationException();
        }
        String token = authorizationToken.substring(7);
        ValidationUtils.isEquals(gameSession.getToken(), token, AuthenticationException::new);

        //4. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);

    }

    private void settleBet(BetNSettleDto betNSettleDto, GameSession gameSession, String traceId,
                           BetNSettleVo betNSettleVo, HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException, InvalidOperatorResponseException, BetNotFoundException, InvalidFormatException, MergedBetDataIntegrityException, InsufficientBalanceException, InternalServerTimeoutRetryException {
        try {
            settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(
                    betNSettleDto.getTransactionId(),
                    betNSettleDto.getRoundId(),
                    gameSession.getVendorId(),
                    gameSession.getVendorPlayerId()
            );
            throw new BetRefundIdempotentViolationException();
        } catch (BetNotFoundException e) {
            ResultType updatedResultType = vendorService.calculateResultType(betNSettleDto.getBetAmount(), betNSettleDto.getWinAmount(), betNSettleDto.getJackpotAmount(), true);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betNSettleDto, updatedResultType, vendorService, httpRequestLog);
            betNSettleVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
        }
    }
}
