package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aviatrix.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(EndPoints.PATH)
public class PromoWinAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorGameDataService vendorGameDataService;
    private final VendorCurrencyDataService vendorCurrencyDataService;
    private final AviatrixPromoPayoutService promoPayoutService;

    @Autowired
    public PromoWinAction(HttpService httpService,
                          GameSessionService gameSessionService,
                          VendorLineService vendorLineService,
                          VendorPlayerDataService vendorPlayerDataService,
                          VendorGameDataService vendorGameDataService,
                          VendorCurrencyDataService vendorCurrencyDataService,
                          AviatrixPromoPayoutService promoPayoutService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorGameDataService = vendorGameDataService;
        this.vendorCurrencyDataService = vendorCurrencyDataService;
        this.promoPayoutService = promoPayoutService;
    }

    @PostMapping(EndPoints.BONUS)
    public ResponseEntity<ResponseVo> promoWin(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String body = httpRequestLog.getRequestBody();

        try {
            PromoWinDto dto = HttpService.convertJsonToDto(body, PromoWinDto.class);

            this.doValidation(dto);

            this.doVerification(dto);

            responseVo = promoPayoutService.payout(dto, httpRequestLog);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setMessage(ResponseCodes.PLAYER_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);
        } catch (DuplicateRequestException duplicateRequestException) {
            if (duplicateRequestException.getCurrency() == null) {
                // The idempotency row was written by the guard but never enriched with a response, so the
                // first attempt died before it paid out — and PromoPayoutServiceImpl's finally calls
                // guard.cleanup(), which clears thread locals without deleting the row. Reporting success
                // here would hand Aviatrix a settled payout that never happened. A 5xx keeps their resend
                // chain alive until the underlying failure clears.
                responseVo.setMessage(ResponseCodes.UNKNOWN_ERROR);
                responseVo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                // A genuine replay: echo the balance the original payout recorded.
                responseVo.setBalance(PromoWinResponseMapper.toMinorUnits(duplicateRequestException.getBalance()));
                responseVo.setCreatedAt(VendorService.returnTime());
            }
            httpService.logError(httpRequestLog, duplicateRequestException);
        } catch (MismatchedInputException |
                 NullPointerException |
                 InvalidRequestException |
                 com.nextgen.core.exception.InvalidRequestException invalidRequestException) {
            responseVo.setMessage(ResponseCodes.INVALID_REQUEST);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (PlayerDisabledException playerDisabledException) {
            responseVo.setMessage(ResponseCodes.PLAYER_BANNED);
            responseVo.setHttpStatus(HttpStatus.FORBIDDEN);
            httpService.logError(httpRequestLog, playerDisabledException);
        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setMessage(ResponseCodes.PRODUCT_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, gameNotSupportedException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setMessage(ResponseCodes.PLATFORM_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setMessage(ResponseCodes.INVALID_PLAYER_CURRENCY);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, currencyNotSupportedException);
        } catch (EntityNotFoundException campaignNotFoundException) {
            responseVo.setMessage(ResponseCodes.INVALID_TRANSACTION);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, campaignNotFoundException);
        } catch (Exception e) {
            responseVo.setMessage(ResponseCodes.UNKNOWN_ERROR);
            responseVo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return new ResponseEntity<>(responseVo, responseVo.getHttpStatus());
    }

    private void doValidation(PromoWinDto dto) throws InvalidRequestException {
        //basic validation
        ValidationUtils.validateRequest(dto);
    }

    /**
     * The only caller authentication on this endpoint.
     *
     * <p><b>Spec debt:</b> Aviatrix's API mandates an {@code X-Auth-Signature} header on every request and
     * we validate it nowhere — there is no validator registered for this vendor, and {@code VendorAuthFilter}
     * does not run for Aviatrix at all because it is absent from the {@code Vendors} registry. That leaves
     * the {@code cid} equality check below as the only thing establishing who the caller is. Note {@code cid}
     * is a non-secret brand identifier that travels in game launch URLs, so this is a consistency check
     * rather than authentication. A proper {@code AviatrixSignatureValidator} covering all four endpoints is
     * tracked separately.
     *
     * <p><b>Player-derived, not session-derived.</b> These checks resolve the player rather than the game
     * session. The session is still corroborated where one exists — see {@link #verifySessionToken} — but
     * it is no longer the source of {@code vendorLineId}, the game code or the currency, because
     * {@code getGameSessionByVendorPlayerUsername} is deprecated and returns the player's most recent
     * session for <em>any</em> game: a promo arriving while the newest session belongs to a different game
     * failed the old {@code productId} comparison spuriously. Resolving the product against
     * {@code vendor_game} has no such dependency on what the player last launched.
     *
     * <p>The lookups mirror what {@code BaseEnricher} does during enrichment; they are repeated here only
     * because enrichment runs too late to reject a request, and because it collapses every missing-row
     * failure into one {@code InternalConfigurationException} that could not be mapped back to Aviatrix's
     * distinct player, product and currency responses.
     */
    private void doVerification(PromoWinDto dto)
            throws InvalidVendorLineException, CredentialNotFoundException, GameNotSupportedException,
            CurrencyNotSupportedException, InvalidPlayerException {

        VendorPlayer vendorPlayer;
        try {
            vendorPlayer = vendorPlayerDataService.getByUsername(dto.getPlayerId());
        } catch (EntityNotFoundException e) {
            throw new InvalidPlayerException(dto.getPlayerId());
        }

        //check cid
        String cid = vendorLineService.getCredentialValueByName(vendorPlayer.getVendorLineId(), Credentials.CID);
        ValidationUtils.isEquals(cid, dto.getCid(), InvalidVendorLineException::new);

        //check the session token belongs to this player, when a session resolves at all
        this.verifySessionToken(dto);

        //check the product is one this vendor offers
        try {
            vendorGameDataService.getByVendorGameCodeAndVendorId(dto.getProductId(), vendorPlayer.getVendorId());
        } catch (EntityNotFoundException e) {
            throw new GameNotSupportedException(dto.getProductId());
        }

        //check the request currency against the player's vendor currency
        String vendorCurrencyCode;
        try {
            vendorCurrencyCode = vendorCurrencyDataService
                    .getByVendorIdAndCurrencyId(vendorPlayer.getVendorId(), vendorPlayer.getCurrencyId())
                    .getVendorCurrencyCode();
        } catch (EntityNotFoundException e) {
            throw new CurrencyNotSupportedException(dto.getCurrency());
        }
        ValidationUtils.isEquals(vendorCurrencyCode, dto.getCurrency(), CurrencyNotSupportedException::new);
    }

    /**
     * Corroborates {@code sessionToken} against the player, without requiring a session to exist.
     *
     * <p>Aviatrix mandates {@code sessionToken} on every promoWin and we previously checked only that it
     * was non-blank. {@code verifyToken} resolves the row by token and applies no expiry logic, so this
     * still holds for a payout arriving after the token has expired — which the spec requires.
     *
     * <p>Absence is tolerated on purpose: a player granted a promo who never launched a game has no
     * session row, and a daily bonus is exactly that case (OAS-5107). Rejecting it would reintroduce the
     * failure this ticket exists to remove. That also means this is a consistency check, not
     * authentication — an unknown token passes. Only an {@code X-Auth-Signature} validator closes that,
     * and it is tracked separately.
     */
    private void verifySessionToken(PromoWinDto dto) throws InvalidPlayerException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.verifyToken(dto.getSessionToken());
        } catch (AuthenticationException noSessionForToken) {
            log.info("Aviatrix promoWin carries no resolvable session, playerId={}, txId={}",
                    dto.getPlayerId(), dto.getTxId());
            return;
        }
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
    }

}
