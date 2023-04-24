package com.nextgen.gameaggregator.vendor.joker.api.token;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.joker.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TokenAction {

    @Autowired
    private HttpService httpService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.TOKEN)
    public TokenVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        TokenVo tokenVo = new TokenVo();
//        tokenVo.setResponseCode(ResponseCodes.SUCCESS);
//        tokenVo.setUsername("TESTPLAYER001");
//        tokenVo.setBalance(1000.00);

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            TokenDto tokenDto = HttpService.convertQueryStringToDtoUrlDecode(body, TokenDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(tokenDto);

            //get rawGameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(tokenDto.getToken());

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, tokenDto, gameSession);

            //return double balance and success code
            tokenVo.setResponseCode(ResponseCodes.SUCCESS);
            tokenVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            tokenVo.setUsername(gameSession.getVendorPlayerUsername());


        } catch (
                InvalidAgentApiCredentialException |
                AuthenticationException |
                InvalidOperatorResponseException |
                CredentialNotFoundException exception
        ) {
            tokenVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } catch (InvalidSignatureException invalidSignatureException) {
            tokenVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
        } catch (NoAvailableLineException noAvailableLineException) {
            tokenVo.setResponseCode(ResponseCodes.INVALID_APPID);
        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if(invalidRequestException.getValidation() != null) {
                tokenVo.setResponseCode(invalidRequestException.getValidation().values().stream().findFirst().orElse(ResponseCodes.OTHER_MESSAGE));
            }else{
                tokenVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
            }
        } catch (Exception exception) {
            tokenVo.setResponseCode(ResponseCodes.OTHER_MESSAGE);
        } finally {
            httpService.end(httpRequestLog, tokenVo);
        }

        return tokenVo;
    }

    private void doValidation(TokenDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, TokenDto dto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, InvalidSignatureException {

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
        ValidationUtils.isEquals(agentCode, dto.getAppid(), NoAvailableLineException::new);

        //Verify received hash
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        VendorService.verifyHash(request.getRequestBody(), secretKey);

    }

}
