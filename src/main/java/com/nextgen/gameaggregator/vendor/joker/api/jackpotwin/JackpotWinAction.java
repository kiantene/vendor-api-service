package com.nextgen.gameaggregator.vendor.joker.api.jackpotwin;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class JackpotWinAction {


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

    @PostMapping(path = EndPoints.JACKPOT_WIN)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
//        commonVo.setResponseCode(ResponseCodes.SUCCESS);
//        commonVo.setBalance(1000.00);

        try{
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            JackpotWinDto jackpotWinDto = HttpService.convertQueryStringToDtoUrlDecode(body, JackpotWinDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(jackpotWinDto);

            //get rawGameSession by player name in lowercase (vendor return in uppercase) and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(jackpotWinDto.getUsername().toLowerCase(), jackpotWinDto.getGamecode());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, jackpotWinDto, gameSession);

            //Process full bet data
            ResultType resultType = jackpotWinDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, jackpotWinDto, resultType, vendorService, body);

            //return double balance and success code
            commonVo.setResponseCode(ResponseCodes.SUCCESS);
            commonVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());

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
        } catch (InvalidSignatureException invalidSignatureException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
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

    private void doValidation(JackpotWinDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, JackpotWinDto jackpotWinDto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, InvalidSignatureException, AuthenticationException {

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(agentCode, jackpotWinDto.getAppid(), NoAvailableLineException::new);

        //Verify received hash
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        VendorService.verifyHash(request.getRequestBody(), secretKey);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, jackpotWinDto.getUsername());
    }

}
