package com.nextgen.gameaggregator.vendor.avatarux.api.betnsettle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetResultLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
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
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetNSettleAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final UnsettledBetService unsettledBetService;
    private final SettledBetService settledBetService;

    public BetNSettleAction(WalletService walletService,
                            HttpService httpService,
                            ValidationService validationService,
                            VendorService vendorService,
                            VendorLineService vendorLineService,
                            GameSessionService gameSessionService,
                            UnsettledBetService unsettledBetService,
                            SettledBetService settledBetService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.unsettledBetService = unsettledBetService;
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
                    walletService.processBet(traceId, gameSession, betNSettleDto, body, httpRequestLog);
                    balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
                    betNSettleVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
                    break;

                case "deposit":
                    //Settle
                    //Check for Bet
                    unsettledBetService.getUnsettledBet(betNSettleDto, betNSettleDto.getRoundId(), gameSession, httpRequestLog);

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

    private void doVerification(BetNSettleDto dto, GameSession gameSession, String body) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException {
        //1. validate vendor username, agent vendor line, player status, and game status
        if (dto.getType().equals("withdraw")) {
            validationService.validateEligibleBet(gameSession, dto.getNativeId());
        }

        //2. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getNativeId(), AuthenticationException::new);

        //3. Verify provider
        String provider = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROVIDER);
        ValidationUtils.isEquals(provider, dto.getProvider());

        //4. Verify Authorization
        String authorizationToken = dto.getAuthorization();
        if (authorizationToken == null || !authorizationToken.startsWith("Bearer ")) {
            throw new AuthenticationException();
        }
        String token = authorizationToken.substring(7);
        ValidationUtils.isEquals(gameSession.getToken(), token, AuthenticationException::new);

        //5. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);

    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, final HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogdup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogdup);
    }

    private void settleBet(BetNSettleDto betNSettleDto, GameSession gameSession, String traceId,
                           BetNSettleVo betNSettleVo, HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, TransactionStillProcessingException, InvalidOperatorResponseException, BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException, InternalServerTimeoutRetryException {

        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), betNSettleDto.getRoundId());

        if (settledBetList == null || settledBetList.isEmpty()) {
            //If not yet Settle
            ResultType updatedResultType = vendorService.calculateResultType(betNSettleDto.getBetAmount(), betNSettleDto.getWinAmount(), betNSettleDto.getJackpotAmount(), true);
            walletService.processBetResult(traceId, gameSession, betNSettleDto, updatedResultType, vendorService, httpRequestLog);
            betNSettleVo.setBalance(getCurrentBalance(traceId, gameSession, httpRequestLog).setScale(2, RoundingMode.DOWN));
        } else {
            //Check if Idempotent Settle
            settledBetIdempotentCheck(betNSettleDto.getTransactionId(), betNSettleDto.getRoundId(), gameSession, traceId, httpRequestLog);
        }
    }

    private void settledBetIdempotentCheck(String dtoTransactionId,
                                           String dtoRoundId,
                                           GameSession gameSession,
                                           String traceId,
                                           HttpRequestLog httpRequestLog) throws
            TransactionStillProcessingException, BetResultIdempotentViolationException, BetNotFoundException,
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {

        String settledBetId = SettledBet.generateId(
                dtoTransactionId,
                dtoRoundId,
                gameSession.getVendorGameId(),
                gameSession.getVendorPlayerId()
        );
        SettledBet settledBet = settledBetService.getById(settledBetId);

        if (settledBet != null) {
            Integer operatorStatus = settledBet.getOperatorStatus();
            if (operatorStatus.equals(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code)) {
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(ResponseCodes.Status.SC_OK.code)) {
                RawBetResultLog betResultLog = new RawBetResultLog();
                betResultLog.setBalance(getCurrentBalance(traceId, gameSession, httpRequestLog));
                throw new BetResultIdempotentViolationException(betResultLog);
            }
        }
    }
}
    