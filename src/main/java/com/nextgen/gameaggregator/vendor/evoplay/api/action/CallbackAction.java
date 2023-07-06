package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.evoplay.api.authenticate.InitService;
import com.nextgen.gameaggregator.vendor.evoplay.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.evoplay.api.endround.WinService;
import com.nextgen.gameaggregator.vendor.evoplay.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(EndPoints.PATH)
public class CallbackAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private InitService initService;
    @Autowired
    private BetService betService;
    @Autowired
    private WinService winService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;

    // Handle incoming API requests
    @PostMapping
    public ResponseVo callback(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();

        String traceId = httpRequestLog.getId();
        GameSession gameSession = new GameSession();

        try {
            String body = httpRequestLog.getRequestBody();
            CallbackDto callbackDto = VendorService.convertBodyToDto(body, CallbackDto.class);
            gameSession = gameSessionService.verifyToken(callbackDto.getToken());

            String projId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJ_ID);
            String key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KEY);

            Optional.ofNullable(projId).orElseThrow(InvalidVendorLineException::new);
            Optional.ofNullable(key).orElseThrow(InvalidVendorLineException::new);

            callbackDto.setProject(projId);
            callbackDto.setVersion(Formats.CALLBACK_VERSION);

            switch (callbackDto.getName().toLowerCase()) {
                case "init" -> {
                    responseVo = initService.init(callbackDto, gameSession, traceId, key);
                }
                case "bet" -> {
                    responseVo = betService.bet(callbackDto, gameSession, body, traceId, key);
                }
                case "win" -> {
                    responseVo = winService.win(callbackDto, gameSession, httpRequestLog, traceId, key);
                }
                case "refund" -> {
                    responseVo = refundService.refund(callbackDto, gameSession, traceId, key);
                }
                // If the header does not match any of the expected values, return an error response
                default -> {
                    throw new InvalidRequestException();
                }
            }

        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE_ERROR);

        } catch (TransactionStillProcessingException e) {
            responseVo.setResponseCode(ResponseCodes.TEMPORARY_ERROR);

        } catch (AuthenticationException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 DisabledVendorLineException |
                 BetNotFoundException |
                 InvalidOperatorResponseException |
                 RecordNotFoundException e) {
            responseVo.setResponseCode(ResponseCodes.PROCESSING_ERROR);

        } catch (InvalidRequestException |
                 InvalidVendorLineException |
                 InvalidPlayerException |
                 InvalidAgentApiCredentialException |
                 CredentialNotFoundException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_REQUEST_ERROR);

        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException e) {
            idempotentSetBalance(traceId, gameSession, responseVo);

        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCodes.UNKNOWN_ERROR);
            responseVo.getError().setNo_refund(Formats.RESEND_CALLBACK);
            httpService.logError(httpRequestLog, e);
        } finally {
            // End the HTTP request logging and return the ResponseVo object
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void idempotentSetBalance(String traceId, GameSession gameSession, ResponseVo responseVo) {
        try {
            ResponseDataVo responseDataVo = new ResponseDataVo();
            responseDataVo.setBalance(walletService.getBalance(traceId, gameSession));
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setData(responseDataVo);
        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCodes.PROCESSING_ERROR);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCodes.UNKNOWN_ERROR);
        }
    }
}
