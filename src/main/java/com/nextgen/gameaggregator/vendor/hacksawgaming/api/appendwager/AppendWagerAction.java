package com.nextgen.gameaggregator.vendor.hacksawgaming.api.appendwager;

import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AppendWagerAction {

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

    @PostMapping(path = EndPoints.APPEND_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        responseDataVo.setBrandUid("testgame3");
        responseDataVo.setCurrency("CNY");
        responseDataVo.setBalance(BigDecimal.valueOf(1000));
        responseVo.setData(responseDataVo);
        responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SUCCESS));
        responseVo.setCode(ResponseCodes.SUCCESS);
        httpService.end(httpRequestLog, responseVo);

        /*
        try {

            AppendWagerDto dto = HttpService.convertJsonToDto(body, AppendWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            RawGameSession rawGameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getBrandUid(), dto.getGameId().toString());
            String toVerifySign = VendorService.getSign(Credentials.BRAND_ID + dto.getWagerId() + Credentials.API_KEY);
            this.doVerification(dto, rawGameSession, toVerifySign);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, rawGameSession);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(rawGameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(rawGameSession.getVendorCurrencyCode());
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

    private void doValidation(AppendWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AppendWagerDto dto, RawGameSession rawGameSession, String toVerifySign)
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
        validationService.validateIllegibleBet(rawGameSession, dto.getBrandUid());

        // Verify currency
        ValidationUtils.isEquals(rawGameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
