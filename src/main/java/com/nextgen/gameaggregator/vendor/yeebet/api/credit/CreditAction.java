package com.nextgen.gameaggregator.vendor.yeebet.api.credit;

import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawBetDetailsProducer;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yeebet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.constant.RequestType;
import com.nextgen.gameaggregator.vendor.yeebet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction {
    @Autowired
    VendorService vendorService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private RawBetDetailsProducer rawBetDetailsProducer;

    @PostMapping(path = EndPoints.DEPOSIT)
    public ResponseEntity<ResponseVo> credit(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        GameSession gameSession = new GameSession();

        BigDecimal balance = null;

        ResponseVo responseVo = new ResponseVo();

        CreditDto creditDto = new CreditDto();

        Integer httpStatus = HttpStatus.SC_OK; //default is 200 status

        try {

            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            creditDto = httpService.convertQueryStringToDtoUrlDecode(body, CreditDto.class);

            creditDto.convertBetToDto();

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // Verify session token
            gameSession = this.getGameSession(traceId, creditDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession, body);

            // 9 - request to update those unsettled transaction to settled status
            if (creditDto.getType().equals(RequestType.SETTLE_REQUEST)) {
                ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false);

                balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

                this.emitRawBetDetail(gameSession, creditDto, httpRequestLog.getGaBetId(), body);

                // set vo
                responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
                responseVo.setResult(ResponseCodes.SUCCESS_CODE);
                responseVo.setSerialnumber(creditDto.getSerialnumber());
                responseVo.setOrderno(traceId);
                responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));

            } else {
                // reject invalid transaction (return error msg to reject the action invalid request)
                // set vo
                responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
                responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);

            }

        } catch (TransactionStillProcessingException |
                 BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);

            // Retrieve the latest wallet balance from Operator
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
            responseVo.setResult(ResponseCodes.SUCCESS_CODE);
            responseVo.setSerialnumber(creditDto.getSerialnumber());
            responseVo.setOrderno(traceId);
            responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));

        } catch (VendorCurrencyNotSupportException |
                 InsufficientBalanceException |
                 InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException |
                 BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);

            httpStatus = HttpStatus.SC_FORBIDDEN; //403 status

            // set vo
            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);

            httpStatus = HttpStatus.SC_FORBIDDEN; //403 status

            // set vo
            responseVo.setDesc(ResponseCodes.PARAMETER_ERROR_MSG);
            responseVo.setResult(ResponseCodes.PARAMETER_ERROR_CODE);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

            httpStatus = HttpStatus.SC_FORBIDDEN; //403 status

            // set vo
            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(httpStatus));
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getBetsDto());
    }

    private void doVerification(CreditDto dto, GameSession gameSession, String queryString) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, GameNotSupportedException, CurrencyNotSupportedException, CredentialNotFoundException, InvalidRequestException {

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getBetsDto().getGameid(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getBetsDto().getCurrency(), CurrencyNotSupportedException::new);

        //Verify received appid is same with credential
        String appid = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_app_id);
        ValidationUtils.isEquals(appid, dto.getAppid(), InvalidRequestException::new);

        // Verify sign value
        String secret_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_secret_key);

        // Trim the string from the beginning until the first "&sign"
        int signIndex = queryString.indexOf("&sign");
        String trimmedString = queryString.substring(0, signIndex);

        // Convert query string into map format with ASCII order
        MultiValueMap<String, String> sortedMultiValueMap = vendorService.convertToSortedMultiValueMap(trimmedString);

        // generate sign value
        String verify_sign = vendorService.generateSign(sortedMultiValueMap, secret_key);

        ValidationUtils.isEquals(verify_sign, dto.getSign(), InvalidRequestException::new);
    }

    private void emitRawBetDetail(GameSession gameSession, CreditDto creditDto, String gaBetId, String body) {
        String vendorBetId = creditDto.getVendorBetId();
        String roundId = creditDto.getRoundId();
        if (gameSession == null) {
            log.warn("Skipping {} raw bet detail emit: gameSession is null vendorBetId={} roundId={}",
                    EndPoints.VENDOR, vendorBetId, roundId);
            return;
        }
        try {
            rawBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(EndPoints.VENDOR)
                    .eventKind(EventKind.RESULT_UPDATE)
                    .vendorBetId(vendorBetId)
                    .gaBetId(gaBetId)
                    .roundId(roundId)
                    .vendorPlayerUsername(gameSession.getVendorPlayerUsername())
                    .agentId(gameSession.getAgentId())
                    .gameCategoryId(gameSession.getGameCategoryId())
                    .bodyFormat(EndPoints.BODY_FORMAT)
                    .requestBody(body)
                    .build());
        } catch (Exception e) {
            log.warn("{} raw bet detail emit failed vendorBetId={} roundId={}: {}",
                    EndPoints.VENDOR, vendorBetId, roundId, e.getMessage());
        }
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        } catch (Exception exception) {

        }

        return balance;
    }

    private GameSession getGameSession(String traceId, CreditDto creditDto) throws GameNotSupportedException, InvalidPlayerException, VendorCurrencyNotSupportException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(creditDto.getUsername());

            // check db game code is stg or not
            if (gameSession.getVendorGameCode().toLowerCase().contains("_stg")) {
                creditDto.getBetsDto().setGameid(creditDto.getBetsDto().getGameid() + "_stg");
            }

            vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(creditDto.getBetsDto().getGameid(), gameSession);

        } catch (AuthenticationException e) {
            gameSession = gameSessionService.generateNewSessionToken(creditDto.getUsername());
            gameSessionService.updateByVendorGameCode(gameSession, creditDto.getGameId());
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }
}
