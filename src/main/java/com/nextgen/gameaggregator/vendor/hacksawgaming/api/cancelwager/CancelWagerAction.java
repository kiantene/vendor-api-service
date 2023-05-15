package com.nextgen.gameaggregator.vendor.hacksawgaming.api.cancelwager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksawgaming.api.wager.WagerDto;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksawgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksawgaming.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.hacksawgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelWagerAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.CANCEL_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();

        try {

            CancelWagerDto dto = HttpService.convertJsonToDto(body, CancelWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Get last game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getBrandUid());

            // Verify data
            this.doVerification(dto, gameSession);

            if(dto.wagerType == 1) {
                /*
                TODO: To handle duplicate bet exception (vendor identify duplicate by round_id an wager_id)
                 Also to retrieve more than 1 records based on same round issue
                 */
                // BetRefundEvent event = walletService.processRefund(traceId, dto.getRoundId(), rawGameSession, body);
            } else if(dto.wagerType == 2) {
                // TODO: cancel (deduct) end wager record
            }

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Set Vendor player username + Balance + Currency
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);
            responseDataVo.setBalance(balance);

        } catch (AuthenticationException |
                 InvalidVendorLineException e
        ) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch(CurrencyNotSupportedException e) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, e);
        } catch(InvalidPlayerException e) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledGameException e) {
            responseVo.setCode(ResponseCodes.GAME_ID_NOT_EXIST);
            httpService.logError(httpRequestLog, e);
        }/* catch (DuplicateExternalTransactionIdException |
                 RecordNotFoundException e
        ) {
            responseVo = this.getCurrentBalanceResponseVo(request, traceId, body);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            responseVo = this.getCurrentBalanceResponseVo(request, traceId, body);
            responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);
            httpService.logError(httpRequestLog, e);
        }*/ catch(DisabledVendorLineException |
                DisabledAgentPlayerException |
                CredentialNotFoundException |
                InvalidRequestException |
                InvalidAgentApiCredentialException |
                InvalidOperatorResponseException |
                JsonProcessingException e
        ) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(CancelWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelWagerDto dto, GameSession gameSession)
            throws InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException, AuthenticationException {

        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        String toVerifySign = VendorService.getSign(brandId + dto.getWagerId() + apiKey);

        // Verify signature
        if(!VendorService.isSameSignature(dto.getSign(), toVerifySign)) {
            throw new InvalidVendorLineException();
        }

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getBrandUid());

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }

    private ResponseVo getCurrentBalanceResponseVo (HttpServletRequest request, String traceId, String body) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        try {
            WagerDto dto = HttpService.convertJsonToDto(body, WagerDto.class);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getBrandUid());

            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(walletService.getBalance(traceId, gameSession));
            responseVo.setData(responseDataVo);

        } catch (InvalidAgentApiCredentialException |
                 JsonProcessingException |
                 InvalidOperatorResponseException e) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
