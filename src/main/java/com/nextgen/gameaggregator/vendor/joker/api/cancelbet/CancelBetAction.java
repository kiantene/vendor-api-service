package com.nextgen.gameaggregator.vendor.joker.api.cancelbet;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
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

    @PostMapping(path = EndPoints.CANCEL_BET)
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
            CancelBetDto cancelBetDto = HttpService.convertQueryStringToDtoUrlDecode(body, CancelBetDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(cancelBetDto);

            //get gameSession by player name in lowercase (vendor return in uppercase) and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(cancelBetDto.getUsername().toLowerCase(), cancelBetDto.getGamecode());

            //Gather require data
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(cancelBetDto.getBetid(), gameSession.getVendorId());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetDto, gameSession);

            //Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRefund(traceId, cancelBetDto.getBetid(), gameSession, body);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(betRefundEvent.getLastBalance().setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (InvalidAgentApiCredentialException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (RecordNotFoundException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (AuthenticationException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (BetNotFoundException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (InvalidOperatorResponseException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (DuplicateExternalTransactionIdException e) {
            commonVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (CredentialNotFoundException e) {
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

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetDto cancelBetDto, GameSession gameSession) throws InvalidRequestException, CredentialNotFoundException {

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(AgentCode, cancelBetDto.getAppid(), InvalidRequestException::new);

    }

}
