package com.nextgen.gameaggregator.vendor.bgaming.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bgaming.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.bgaming.api.endround.EndRoundService;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BetService betService;
    @Autowired
    private EndRoundService endRoundService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping(path = EndPoints.PLAY)
    public ResponseEntity<ResponseVo> action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        CommonDto commonDto = null;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);

            // Validate the commonDto object
            this.doValidation(commonDto);

            // Get vendor player details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getUserId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, httpRequestLog, request);

            // Handle the action and return the resulting value
            this.serviceHandling(commonDto, httpRequestLog, request, responseVo, gameSession);

            responseVo.setHttpStatus(HttpStatus.SC_OK);

        } catch (InvalidSignatureException e) {
            responseVo.setResponseCodes(ResponseCodes.REQUEST_SIGN_DOES_NOT_MATCH);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException |
                 DisabledVendorLineException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 BetNotFoundException |
                 InvalidPlayerException |
                 JsonProcessingException e) {
            responseVo.setResponseCodes(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCodes(ResponseCodes.FUND_NOT_ENOUGH);
            httpService.logError(httpRequestLog, e);

        } catch (TransactionStillProcessingException | BetResultIdempotentViolationException e) {
            handleDuplicateBet(commonDto, httpRequestLog, request, responseVo);
            responseVo.setResponseCodes(ResponseCodes.SUCCESS);
            httpService.logError(httpRequestLog, e);

        } catch (CurrencyNotSupportedException e) {
            responseVo.setResponseCodes(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            responseVo.setResponseCodes(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            if (commonDto.getActions() != null && commonDto.getActions().isEmpty()) {
                responseVo.setTransactions(new LinkedList<>());
            }
            httpService.end(httpRequestLog, responseVo);
        }
        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(responseVo.getHttpStatus()));
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws JsonProcessingException, CredentialNotFoundException, InvalidSignatureException {
        // Convert Body to Map for signature check
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> bodyObj = mapper.readValue(httpRequestLog.getRequestBody(), Map.class);

        // Verify Signature key from vendor given
        String authToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AUTH_TOKEN);
        VendorService.verifySign(authToken, new Gson().toJson(bodyObj), request.getHeader("X-REQUEST-SIGN"));
    }

    private void serviceHandling(CommonDto commonDto, HttpRequestLog httpRequestLog, HttpServletRequest request, ResponseVo responseVo, GameSession gameSession) throws InvalidRequestException, InvalidAgentApiCredentialException, InvalidPlayerException, AuthenticationException, BetResultIdempotentViolationException, DisabledAgentPlayerException, DisabledGameException, InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, DisabledVendorLineException, MergedBetDataIntegrityException, BetNotFoundException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, GameNotSupportedException, VendorCurrencyNotSupportException {

        if (!commonDto.getFinished() && (commonDto.getActions() == null || commonDto.getActions().isEmpty())) {
            // No action , Get balance
            balanceService.balance(commonDto, httpRequestLog, request, responseVo);
        } else if (commonDto.getFinished() && (commonDto.getActions() == null || commonDto.getActions().isEmpty())) {
            endRoundService.endRound(commonDto, null, request, responseVo, gameSession);
        } else {
            for (ActionDto actionDto : commonDto.getActions()) {
                commonDto.setActionDto(actionDto);
                switch (actionDto.getAction()) {
                    case "bet":
                        betService.bet(commonDto, request, responseVo, gameSession);
                        // If this is last bet action and finished true then will process to end round
                        if (!(commonDto.getFinished() && (commonDto.getActions().indexOf(actionDto) == (commonDto.getActions().size() - 1)))) {
                            break;
                        }
                    case "win":
                        endRoundService.endRound(commonDto, actionDto, request, responseVo, gameSession);
                        break;
                    default:
                        throw new InvalidRequestException();
                }
            }
        }
    }

    private void handleDuplicateBet(CommonDto commonDto, HttpRequestLog httpRequestLog, HttpServletRequest request, ResponseVo responseVo) {
        try {
            balanceService.balance(commonDto, httpRequestLog, request, responseVo);
        } catch (Exception e) {
            responseVo.setResponseCodes(ResponseCodes.UNKNOWN_ERROR);
        }
    }
}