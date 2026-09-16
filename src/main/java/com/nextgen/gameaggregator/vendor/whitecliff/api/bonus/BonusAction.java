package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.whitecliff.api.bet.DebitDto;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.Credentials;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseError;
import com.nextgen.gameaggregator.vendor.whitecliff.service.VendorService;
import com.nextgen.gameaggregator.vendor.whitecliff.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Objects;


@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BonusAction {
    /** {@code game_category} id for live casino. Matches the checks in {@code DebitDto}/{@code CreditDto}. */
    private static final Integer LIVE_GAME_CATEGORY_ID = 5;

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorLineService vendorLineService;
    private SettledBetService settledBetService;
    private final WhiteCliffBonusPayoutService bonusPayoutService;

    @Autowired
    public BonusAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, ValidationService validationService, VendorService vendorService, VendorLineService vendorLineService, SettledBetService settledBetService, WhiteCliffBonusPayoutService bonusPayoutService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorLineService = vendorLineService;
        this.settledBetService = settledBetService;
        this.bonusPayoutService = bonusPayoutService;
    }

    @PostMapping(path = EndPoints.BONUS)
    public ResponseVo bonusAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        VendorService vendorService = new VendorService(gameSessionService);


        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        //Get header for Validation
        String secretKey = request.getHeader("secret-key");

        try {
            //Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BonusDto bonusDto = HttpService.convertJsonToDto(body, BonusDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(bonusDto.getSid());

            // Validate request parameters (Non-database calls)
            this.doValidation(bonusDto);

            // Check game category to set game code
            bonusDto.setGameCategory(gameSession.getGameCategoryId());

            this.doVerification(bonusDto, gameSession, secretKey);

            Integer idempotentCheckAfterSettle = this.settledBetIdempotentCheckBonus(gameSession, bonusDto);
            if (idempotentCheckAfterSettle == 1) {
                throw new BetResultIdempotentViolationException();
            }

            if (BonusPayoutRequestMapper.isPromoPayout(bonusDto.getType())) {
                this.logGameCodeMismatch(bonusDto, gameSession, traceId);
                // Promotion and Jackpot are free, non-billable payouts: they belong in
                // promo_payout_history and go to the operator via /v1/promo/payout, not into bet history.
                responseVo = bonusPayoutService.payout(
                        BonusPayoutRequest.builder()
                                .bonus(bonusDto)
                                .vendorPlayerUsername(gameSession.getVendorPlayerUsername())
                                .vendorCurrency(gameSession.getVendorCurrencyCode())
                                .build(),
                        httpRequestLog);
            } else {
                // In Game Bonus is won inside a round, so it stays a bet win.
                BigDecimal balance = walletService.processBetResult(
                        traceId, gameSession, bonusDto, ResultType.BET_WIN, vendorService, httpRequestLog);
                responseVo.setBalance(balance);
                responseVo.setStatus(ResponseCodes.SUCCESS);
            }

        } catch (GameNotSupportedException |
                 DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidAgentApiCredentialException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INVALID_DEBIT);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.DUPLICATE_BONUS);
            httpService.logError(httpRequestLog, e);
        } catch (DuplicateRequestException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(e.getCurrency() == null ? ResponseError.UNKNOWN_ERROR : ResponseError.DUPLICATE_BONUS);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.ACCESS_DENIED);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INVALID_USER);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    /**
     * Warns when a promo bonus names a game other than the one the session was launched with.
     *
     * <p>Observation only: nothing is rejected and the session is not re-pointed. {@code /bonus} never
     * reconciles the session against the request the way {@code DebitAction} and {@code CreditAction} do,
     * so {@code GameSession.vendorGameCode} is the last game the session was pointed at rather than
     * necessarily the game this bonus belongs to, and the payout carries no game either way. Rejecting on
     * a mismatch is not an option — PGSoft's equivalent check was disabled under GA-119 because vendors
     * legitimately transact against a game other than the session's.
     *
     * <p>Live tables can never match and are logged separately: the session holds WhiteCliff's
     * {@code table_id} (what game launch sends for category 5, e.g. {@code "baccarat0001"}) while
     * {@code /bonus} carries only the numeric {@code game_id} and has no {@code table_id} field. That line
     * answers a different question — whether live tables produce promo bonuses at all.
     */
    private void logGameCodeMismatch(BonusDto bonusDto, GameSession gameSession, String traceId) {
        String requestGameCode = bonusDto.getGameId();
        String sessionGameCode = gameSession.getVendorGameCode();

        if (Objects.equals(requestGameCode, sessionGameCode)) {
            return;
        }

        if (LIVE_GAME_CATEGORY_ID.equals(gameSession.getGameCategoryId())) {
            log.warn("[MISMATCH] VendorGameCode not comparable on live table. TraceId: {}, Request game_id: {}, Session table_id: {}",
                    traceId, requestGameCode, sessionGameCode);
            return;
        }

        log.warn("[MISMATCH] VendorGameCode: TraceId: {}, Request: {}, Session: {}, GameCategory: {}",
                traceId, requestGameCode, sessionGameCode, gameSession.getGameCategoryId());
    }

    private void doValidation(BonusDto bonusDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(bonusDto);
    }

    private void doVerification(BonusDto bonusDto, GameSession gameSession, String secretKey)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidRequestException,
            GameNotSupportedException,
            InvalidSignatureException {

        // 1. Verify Username, GameCode, CurrencyCode
        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorToken()), String.valueOf(bonusDto.getUserId()), InvalidPlayerException::new);

        // 2. Validate secret key from header
        String credentialKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(credentialKey, secretKey, InvalidSignatureException::new);

        //Validate Prd_id
        String prdId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PRODUCT_ID);
        ValidationUtils.isEquals(String.valueOf(bonusDto.getPrdId()), prdId);

    }


    public Integer settledBetIdempotentCheckBonus(GameSession gameSession, BonusDto dto) {

        Long vendorPlayerId = gameSession.getVendorPlayerId();
        SettledBet settledBet;
        Integer betCheck = 0;

        try {

            settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, dto.getExternalTransactionId());

            if (settledBet != null) { // duplicate request found in settled_bet
                betCheck = 1;
            }
        } catch (BetNotFoundException betNotFoundException) {

        }

        return betCheck;
    }


}
