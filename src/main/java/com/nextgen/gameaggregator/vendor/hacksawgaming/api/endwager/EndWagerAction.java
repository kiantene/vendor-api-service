package com.nextgen.gameaggregator.vendor.hacksawgaming.api.endwager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksawgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksawgaming.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.hacksawgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class EndWagerAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.END_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        responseDataVo.setBrandUid("testgame3");
        responseDataVo.setCurrency("CNY");
        responseDataVo.setBalance(BigDecimal.valueOf(1000));
        httpService.end(httpRequestLog, responseVo);
        /*
        try {

            EndWagerDto dto = HttpService.convertJsonToDto(body, EndWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getBrandUid(), dto.getGameId().toString());
            String toVerifySign = VendorService.getSign(Credentials.BRAND_ID + dto.getWagerId() + Credentials.API_KEY);
            this.doVerification(dto, gameSession, toVerifySign);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);

            // Set BalanceDataWalletVo Object
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SUCCESS));
            responseVo.setCode(ResponseCodes.SUCCESS);

        } catch(InvalidRequestException |
                DateTimeParseException |
                CurrencyNotSupportedException |
                JsonProcessingException |
                NullPointerException |
                IllegalArgumentException e
        ) {

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
        }
        httpService.end(httpRequestLog, responseVo);

         */

        return responseVo;

    }

    private void doValidation(EndWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(EndWagerDto dto, GameSession gameSession, String toVerifySign)
            throws InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException {

        // Verify signature
        if(!VendorService.isSameSignature(dto.getSign(), toVerifySign)) {
            throw new InvalidVendorLineException();
        }

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, dto.getBrandUid());

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
