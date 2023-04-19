package com.nextgen.gameaggregator.vendor.joker.api.settlebet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.joker.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.RoundingMode;

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

    @PostMapping(path = EndPoints.SETTLE_BET)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
//        commonVo.setBalance(1000.00);
//        commonVo.setResponseCode(ResponseCodes.SUCCESS);

        try{
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            SettleBetDto settleBetDto = HttpService.convertQueryStringToDtoUrlDecode(body, SettleBetDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(settleBetDto);

            //get gameSession by player name in lowercase (vendor return in uppercase) and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleBetDto.getUsername().toLowerCase(), settleBetDto.getGamecode());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(settleBetDto, gameSession);

            //Process full bet data
            SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, settleBetDto, body);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(settledBetEvent.getLastBalance().setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (
                InvalidAgentApiCredentialException |
                 AuthenticationException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException |
                 InsufficientBalanceException |
                 InvalidOperatorResponseException |
                 BetNotFoundException |
                 CouchbaseDataIntegrityException |
                 CredentialNotFoundException |
                 DisabledVendorLineException |
                 InvalidPlayerException exception
        ) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (NoAvailableLineException noAvailableLineException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_APPID);
        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if(invalidRequestException.getValidation() != null) {
                commonVo.setResponseCode(invalidRequestException.getValidation().values().stream().findFirst().orElse(ResponseCodes.OTHER_MESSAGE));
            }else{
                commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
            }
        } catch (Exception exception) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(SettleBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleBetDto settleBetDto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException {

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(AgentCode, settleBetDto.getAppid(), NoAvailableLineException::new);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, settleBetDto.getUsername());
    }

}
