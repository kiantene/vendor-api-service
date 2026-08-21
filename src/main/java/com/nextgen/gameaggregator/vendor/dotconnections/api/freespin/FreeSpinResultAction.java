package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.exception.InvalidProviderException;
import com.nextgen.gameaggregator.vendor.dotconnections.service.VendorService;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class FreeSpinResultAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private DotConnectionsFreeSpinResultHandler freeSpinResultHandler;

    @PostMapping(path = EndPoints.FREE_SPIN_RESULT)
    public ResponseVo freeSpinResult(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();

        GameSession gameSession = null;

        try {

            String body = httpRequestLog.getRequestBody();

            FreeSpinResultDto dto = HttpService.convertJsonToDto(body, FreeSpinResultDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getBrandUid(), dto.getGameId());

            // Verify data
            this.doVerification(dto, gameSession);

            // Credit the free round win against the campaign the freespin_id was granted from.
            // Replaces the previous BET_WIN booking: a free round has no stake, and the operator tracks
            // it through /v1/promo/payout rather than /wallet/bet_result.
            responseVo = freeSpinResultHandler.process(dto);

        } catch (InvalidSignatureException signErrorException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, signErrorException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setCode(ResponseCodes.INVALID_BRAND_UID);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setCode(ResponseCodes.GAME_ID_NOT_EXIST);
            httpService.logError(httpRequestLog, gameNotSupportedException);

        } catch (DuplicateRequestException duplicateRequestException) {
            // Replay of a wager_id we have already paid out — echo the balance recorded at the time.
            ResponseDataVo responseDataVo = new ResponseDataVo();
            responseDataVo.setBrandUid(gameSession != null ? gameSession.getVendorPlayerUsername() : null);
            responseDataVo.setCurrency(duplicateRequestException.getCurrency());
            responseDataVo.setBalance(duplicateRequestException.getBalance());
            responseVo.setData(responseDataVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, duplicateRequestException);

        } catch (EntityNotFoundException campaignNotFoundException) {
            // No CampaignPlayer carries this freespin_id as its grant reference.
            responseVo.setCode(ResponseCodes.FREE_SPIN_ID_NOT_EXIST);
            httpService.logError(httpRequestLog, campaignNotFoundException);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                responseVo.setCode(
                        invalidRequestException.getValidation()
                                .entrySet()
                                .stream()
                                .findFirst()
                                .map(Map.Entry::getValue) // get the value of the first element
                                .orElse(ResponseCodes.REQUEST_PARAM_ERROR)
                );

            } else {
                responseVo.setCode(ResponseCodes.REQUEST_PARAM_ERROR);

            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (InvalidProviderException invalidProviderException) {
            responseVo.setCode(ResponseCodes.INVALID_PROVIDER);
            httpService.logError(httpRequestLog, invalidProviderException);

        } catch (CredentialNotFoundException |
                 JsonProcessingException |
                 InternalConfigurationException systemErrorException) {
            // InternalConfigurationException is what BaseEnricher raises when brand_uid, its vendor line
            // or its currency cannot be resolved — a configuration fault on our side, not the caller's.
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, systemErrorException);

        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        return responseVo;

    }

    private void doValidation(FreeSpinResultDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(FreeSpinResultDto dto, GameSession gameSession)
            throws
            InvalidPlayerException,
            CurrencyNotSupportedException,
            CredentialNotFoundException,
            InvalidSignatureException,
            InvalidProviderException,
            GameNotSupportedException {

        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        String toVerifySign = VendorService.getSign(brandId + dto.getWagerId() + apiKey);

        String providerCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROVIDER_CODE);

        // Verify signature
        VendorService.isSameSignature(dto.getSign(), toVerifySign);

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getBrandUid(), InvalidPlayerException::new);

        // Verify provider
        if (!dto.getProvider().equals(providerCode)) {
            throw new InvalidProviderException();
        }

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);

    }
}
