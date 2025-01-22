package com.nextgen.gameaggregator.vendor.koolbet.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.api.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.koolbet.api.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
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
public class TokenAction {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    private final VendorLineService vendorLineService;

    @Autowired
    public TokenAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
    }


    @PostMapping(path = EndPoints.TOKEN)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo responseVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertJsonToDto(body, CommonDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            //get rawGameSession by token id
            GameSession gameSession = gameSessionService.verifyToken(commonDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, commonDto, gameSession);

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //return double balance and success code
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            responseVo.setUsername(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getCurrencyCode());
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, CommonDto dto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, InvalidSignatureException {

//        //Verify received agent code is the same from credential
//        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
//        ValidationUtils.isEquals(agentCode, dto.token(), NoAvailableLineException::new);
//
//        //Verify received hash
//        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
//        VendorService.verifyHash(request.getRequestBody(), secretKey);

    }
}
