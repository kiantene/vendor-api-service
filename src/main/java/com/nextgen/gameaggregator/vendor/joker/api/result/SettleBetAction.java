package com.nextgen.gameaggregator.vendor.joker.api.result;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SettleBetAction {


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
    private VendorService vendorService;
    @Autowired
    private UnsettledBetService unsettledBetService;

    @PostMapping(path = EndPoints.SETTLE_BET)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo commonVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            SettleBetDto settleBetDto = HttpService.convertQueryStringToDtoUrlDecode(body, SettleBetDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(settleBetDto);

            //get rawGameSession by player name in lowercase (vendor return in uppercase) and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleBetDto.getUsername().toLowerCase(), settleBetDto.getGamecode());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, settleBetDto, gameSession);

            //Process full bet data
            ResultType resultType = settleBetDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.END;
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleBetDto, resultType, vendorService, httpRequestLog);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (
                InvalidAgentApiCredentialException |
                DisabledAgentPlayerException |
                MergedBetDataIntegrityException |
                DisabledGameException |
                InsufficientBalanceException |
                CredentialNotFoundException |
                DisabledVendorLineException |
                InvalidPlayerException exception
        ) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (AuthenticationException authenticationException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_TOKEN);
        } catch (BetNotFoundException betNotFoundException) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (InvalidSignatureException invalidSignatureException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
        } catch (NoAvailableLineException noAvailableLineException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_APPID);
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

    private void doValidation(SettleBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, SettleBetDto settleBetDto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, InvalidSignatureException, AuthenticationException {

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(agentCode, settleBetDto.getAppid(), NoAvailableLineException::new);

        //Verify received hash
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        VendorService.verifyHash(request.getRequestBody(), secretKey);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, settleBetDto.getUsername());
    }

}
