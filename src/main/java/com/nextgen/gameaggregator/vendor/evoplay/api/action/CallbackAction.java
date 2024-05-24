package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.api.authenticate.InitService;
import com.nextgen.gameaggregator.vendor.evoplay.api.balanceIncrease.BalanceIncreaseService;
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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
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
    @Autowired
    private BalanceIncreaseService balanceIncreaseService;

    // Handle incoming API requests
    @PostMapping
    public ResponseVo callback(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();

        String traceId = httpRequestLog.getId();
        CallbackDto callbackDto = new CallbackDto();
        GameSession gameSession = new GameSession();

        try {
            String body = httpRequestLog.getRequestBody();

            // convert body into raw Map data
            Map<String, Object> rawData = VendorService.convertBodyToDto(body, LinkedHashMap.class);

            // Mapping raw Map data into Dto
            callbackDto = new ModelMapper().map(rawData, CallbackDto.class);

            // Increase Balance request Vendor didn't send token and signature, so we get gameSession by vendorPlayerUsername and skip verified signature
            if (callbackDto.getName().toLowerCase().equals("balanceincrease")) {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(callbackDto.getData().getUser_id());

            } else {
                // get gameSession
                gameSession = gameSessionService.verifyToken(callbackDto.getToken());

                // use raw body Map data verify signature
                verifySignature(gameSession, rawData, callbackDto);
            }


            switch (callbackDto.getName().toLowerCase()) {
                case "init" -> {
                    responseVo = initService.init(callbackDto, gameSession, traceId, httpRequestLog);
                }
                case "bet" -> {
                    responseVo = betService.bet(callbackDto, gameSession, body, traceId, httpRequestLog);
                }
                case "win" -> {
                    responseVo = winService.win(callbackDto, gameSession, httpRequestLog, traceId);
                }
                case "refund" -> {
                    responseVo = refundService.refund(callbackDto, gameSession, traceId, httpRequestLog);
                }
                case "balanceincrease" -> {
                    responseVo = balanceIncreaseService.balanceIncrease(callbackDto, gameSession, traceId, httpRequestLog);
                }
                // If the header does not match any of the expected values, return an error response
                default -> throw new InvalidRequestException();
            }

        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (TransactionStillProcessingException e) {
            responseVo.setResponseCode(ResponseCodes.TEMPORARY_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidOperatorResponseException e) {
            if (e.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code) && callbackDto.getName().equalsIgnoreCase("refund")) {
                idempotentSetBalance(traceId, gameSession, responseVo, httpRequestLog);
            } else {
                responseVo.setResponseCode(ResponseCodes.PROCESSING_ERROR);
            }
            httpService.logError(httpRequestLog, e);

        } catch (BetNotFoundException e) {
            if (callbackDto.getName().equalsIgnoreCase("refund")) {
                idempotentSetBalance(traceId, gameSession, responseVo, httpRequestLog);
            } else {
                responseVo.setResponseCode(ResponseCodes.PROCESSING_ERROR);
                httpService.logError(httpRequestLog, e);
            }
        } catch (AuthenticationException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 DisabledVendorLineException |
                 RecordNotFoundException e) {
            responseVo.setResponseCode(ResponseCodes.PROCESSING_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException |
                 InvalidVendorLineException |
                 InvalidPlayerException |
                 InvalidAgentApiCredentialException |
                 CredentialNotFoundException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCodes.INVALID_REQUEST_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException e) {
            idempotentSetBalance(traceId, gameSession, responseVo, httpRequestLog);

        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            // End the HTTP request logging and return the ResponseVo object
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void idempotentSetBalance(String traceId, GameSession gameSession, ResponseVo responseVo, HttpRequestLog httpRequestLog) {
        try {
            ResponseDataVo responseDataVo = new ResponseDataVo();
            responseDataVo.setBalance(walletService.getBalance(traceId, gameSession, httpRequestLog));
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setData(responseDataVo);
        } catch (InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCodes.PROCESSING_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        }
    }

    private void verifySignature(GameSession gameSession, Map<String, Object> rawData, CallbackDto callbackDto) throws
            InvalidVendorLineException,
            CredentialNotFoundException,
            AuthenticationException {

        String projId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJ_ID);
        String key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KEY);

        Optional.ofNullable(projId).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(key).orElseThrow(InvalidVendorLineException::new);

        // Verify Signature
        rawData.remove("signature");
        rawData.put("project", projId);
        rawData.put("version", Formats.CALLBACK_VERSION);
        VendorService.rearrangeMap(rawData);

        MultiValueMap<String, String> formData = VendorService.flattenMapIntoMultiValueMap(rawData, "");
        String signature = VendorService.md5(VendorService.buildSignature(formData, key));

        ValidationUtils.isEquals(signature, callbackDto.getSignature(), AuthenticationException::new);
    }
}
