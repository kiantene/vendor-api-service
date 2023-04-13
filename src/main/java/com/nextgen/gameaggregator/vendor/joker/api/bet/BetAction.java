package com.nextgen.gameaggregator.vendor.joker.api.bet;

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

import javax.servlet.http.HttpServletRequest;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {


    @Autowired
    private HttpService httpService;

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.BET)
    public CommonVo balance(HttpServletRequest request) throws InvalidRequestException {
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
            BetDto betDto = HttpService.convertQueryStringToDtoUrlDecode(body, BetDto.class);

            if(betDto.getUsername().toLowerCase().equals("dr6nm")) {
                throw new InvalidRequestException();
            }

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            //get gameSession by player name in lowercase (vendor return in uppercase) and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getUsername().toLowerCase(), betDto.getGamecode());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            //Process full bet data
            SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, betDto, body);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(settledBetEvent.getLastBalance().setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (InvalidAgentApiCredentialException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (AuthenticationException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (DisabledAgentPlayerException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (MergedBetDataIntegrityException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (DisabledGameException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (InsufficientBalanceException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (InvalidOperatorResponseException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (BetNotFoundException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (CouchbaseDataIntegrityException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (CredentialNotFoundException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (DisabledVendorLineException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (InvalidPlayerException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if(invalidRequestException.getValidation() != null) {
                commonVo.setResponseCode(invalidRequestException.getValidation().values().stream().findFirst().orElse(ResponseCodes.OTHER_MESSAGE));
            }else{
                commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
            }
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto betDto, GameSession gameSession) throws InvalidRequestException, CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException {

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(AgentCode, betDto.getAppid(), InvalidRequestException::new);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, betDto.getUsername());
    }

}
