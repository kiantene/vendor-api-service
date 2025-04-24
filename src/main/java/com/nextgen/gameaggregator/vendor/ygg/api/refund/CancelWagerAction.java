package com.nextgen.gameaggregator.vendor.ygg.api.refund;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelWagerAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public CancelWagerAction(HttpService httpService, GameSessionService gameSessionService,
                             WalletService walletService,
                             VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    @GetMapping(path = EndPoints.CANCEL_BET)
    public CancelWagerVo cancelWager(HttpServletRequest request) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CancelWagerDto dto = new CancelWagerDto();

        CancelWagerVo responseVo = new CancelWagerVo();

        DataVo data = new DataVo();

        String vendorCurrencyCode = "";

        GameSession gameSession = new GameSession();
        try {
            // Log Request Body
            httpRequestLog.setRequestBody(request.getQueryString());

            // Convert query string to DTO
            dto = HttpService.convertQueryStringToDto(request.getQueryString(), CancelWagerDto.class);

            // Do validation
            ValidationUtils.validateRequest(dto);

            try {
                // Get Session by playerUsername
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
                // Do verification
                doVerification(dto, gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getPlayerId()); //generate new token
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            // Do verification
            doVerification(dto, gameSession);

            // Process Refund and get new balance
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            data.setBalance(balance);
            data.setOrganization(dto.getOrg());
            data.setPlayerId(dto.getPlayerId());
            data.setCurrency(gameSession.getVendorCurrencyCode());

            // Set response Vodata
            responseVo.setData(data);
            responseVo.setCode(ResponseCode.SUCCESS.code);

        } catch (BetResultIdempotentViolationException e) {
            //Check if bet is refunded
            if (e.getStatus().equals(BetStatus.REFUNDED.code)) {
                // Return original result when idempotent
                data.setCurrency(vendorCurrencyCode);
                data.setOrganization(dto.getOrg());
                data.setBalance(e.getBalance());
                data.setPlayerId(dto.getPlayerId());

                // Set response code and Vodata
                responseVo.setCode(ResponseCode.SUCCESS.code);
                responseVo.setData(data);
            } else {
                responseVo.setResponseCode(ResponseCode.ERROR);
            }
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            // Get player wallet balance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
            //Check if bet is refunded

            // Return original result when idempotent
            data.setCurrency(vendorCurrencyCode);
            data.setOrganization(dto.getOrg());
            data.setBalance(balance);
            data.setPlayerId(dto.getPlayerId());

            // Set response code and Vodata
            responseVo.setCode(ResponseCode.SUCCESS.code);
            responseVo.setData(data);

            httpService.logError(httpRequestLog, e);
        } catch (BetRefundIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_AUTHORIZED);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doVerification(CancelWagerDto dto, GameSession gameSession) throws InvalidPlayerException {

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);


    }
}
