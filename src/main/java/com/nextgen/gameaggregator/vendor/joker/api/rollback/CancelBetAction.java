package com.nextgen.gameaggregator.vendor.joker.api.rollback;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.joker.service.VendorService;
import com.nextgen.gameaggregator.vendor.joker.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelBetAction {


    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo commonVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CancelBetDto cancelBetDto = HttpService.convertQueryStringToDtoUrlDecode(body, CancelBetDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(cancelBetDto);

            //get rawGameSession by player name in lowercase (vendor return in uppercase) and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(cancelBetDto.getUsername().toLowerCase(), cancelBetDto.getGamecode());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, cancelBetDto, gameSession);

            //Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelBetDto, gameSession, vendorService);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            //commonVo.setBalance(betRefundEvent.getLastBalance().setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (
                InvalidAgentApiCredentialException |
                InvalidOperatorResponseException |
                CredentialNotFoundException exception
        ) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
            httpService.logError(httpRequestLog, exception);
        } catch (AuthenticationException authenticationException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_TOKEN);
        } catch (InvalidSignatureException invalidSignatureException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
        } catch (NoAvailableLineException noAvailableLineException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_APPID);
        } catch (
                RecordNotFoundException |
                BetRefundIdempotentViolationException successException
        ) {
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance((double) 0);
        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                commonVo.setResponseCode(
                        invalidRequestException.getValidation()
                                .entrySet()
                                .stream()
                                .findFirst()
                                .map(Map.Entry::getValue) // get the value of the first element
                                .orElse(ResponseCodes.INVALID_PARAMETERS)
                );
            } else {
                commonVo.setResponseCode(ResponseCodes.INVALID_PARAMETERS);
            }
        } catch (Exception exception) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, CancelBetDto cancelBetDto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, InvalidSignatureException {

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(agentCode, cancelBetDto.getAppid(), NoAvailableLineException::new);

        //Verify received hash
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        VendorService.verifyHash(URLDecoder.decode(request.getRequestBody(), StandardCharsets.UTF_8), secretKey);

    }

}
