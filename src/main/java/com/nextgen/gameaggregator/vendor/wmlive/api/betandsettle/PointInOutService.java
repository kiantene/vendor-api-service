package com.nextgen.gameaggregator.vendor.wmlive.api.betandsettle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.wmlive.api.action.GeneralActionDto;
import com.nextgen.gameaggregator.vendor.wmlive.api.vo.DataVo;
import com.nextgen.gameaggregator.vendor.wmlive.api.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.wmlive.constant.BetType;
import com.nextgen.gameaggregator.vendor.wmlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.wmlive.constant.ResponseCode;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@Service
public class PointInOutService {

    private final VendorService vendorService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;

    @Autowired
    public PointInOutService(VendorService vendorService, HttpService httpService,
                             ValidationService validationService,
                             GameSessionService gameSessionService,
                             WalletService walletService, VendorLineService vendorLineService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
    }

    public ResponseVo pointInOut(GeneralActionDto generalActionDto, String traceId, HttpRequestLog httpRequestLog) {

        ResponseVo responseVo = new ResponseVo();

        BigDecimal balance = BigDecimal.ZERO;

        try {
            String body = httpRequestLog.getRequestBody();

            PointInOutDto pointInOutDto = new ModelMapper().map(generalActionDto, PointInOutDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(pointInOutDto);

            // Get GameSession with username
            GameSession gameSession;

            // Differentiate between point in and point out
            if (pointInOutDto.getCode().equals(BetType.POINTOUT)) {// If code is 2: 扣点 will process bet
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(pointInOutDto.getUser());
                // validate vendor username, agent vendor line, player status, and game status
                validationService.validateEligibleBet(gameSession, pointInOutDto.getUser());
                //verify session
                this.doVerification(pointInOutDto, gameSession);

                if (pointInOutDto.getCategory().equals(BetType.TIPS)) {
                    //Process Tips
                    ResultType resultType = vendorService.calculateResultType(pointInOutDto.getBetAmount(), pointInOutDto.getWinAmount(), pointInOutDto.getJackpotAmount(), true);
                    balance = walletService.processBetResult(traceId, gameSession, pointInOutDto, resultType, vendorService, httpRequestLog);
                } else {
                    //process bet
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, pointInOutDto, body, httpRequestLog);
                    balance = betEvent.getLastBalance();
                }

            } else if (pointInOutDto.getCode().equals(BetType.POINTIN)) { //if code is 1: 加点 will process result

                // Try to catch if session is expired and generate new session
                try {
                    //find game session
                    gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(pointInOutDto.getUser());
                } catch (AuthenticationException authenticationException) {
                    //catch authentication error thrown and regenerate token
                    gameSession = gameSessionService.generateNewSessionToken(pointInOutDto.getUser());
                    gameSessionService.updateByVendorGameCode(gameSession, pointInOutDto.getGtype());
                    gameSession.setToken(traceId);
                    gameSession.setVendorToken(traceId);


                }
                //verify session
                doVerification(pointInOutDto, gameSession);
                //Get resultType and process to settle
                ResultType resultType = vendorService.calculateResultType(pointInOutDto.getBetAmount(), pointInOutDto.getWinAmount(), pointInOutDto.getJackpotAmount(), false);
                balance = walletService.processBetResult(traceId, gameSession, pointInOutDto, resultType, vendorService, httpRequestLog);
            } else {
                //For code 3: 重对加点, 4: 重对扣点, 5: 重新派彩 our side reject resettle.
                throw new InvalidRequestException();
            }
            // Set response
            DataVo pointInOutVo = new DataVo(pointInOutDto, balance);
            responseVo.setResult(pointInOutVo);

        } catch (BetResultIdempotentViolationException | InvalidOperatorResponseException |
                 TransactionStillProcessingException | BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR_BLOCKED);
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR_OVERDRAFT);
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR_NOT_AUTHORIZED);
        } catch (CredentialNotFoundException | CredentialException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.CREDENTIAL_ERROR);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR);
        }
        return responseVo;
    }

    private void doValidation(PointInOutDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(PointInOutDto dto, GameSession gameSession) throws
            CredentialNotFoundException, CredentialException {

        //5. Verify received signature is same with credential signature
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE);
        ValidationUtils.isEquals(token, dto.getSignature(), CredentialException::new);
    }
}
