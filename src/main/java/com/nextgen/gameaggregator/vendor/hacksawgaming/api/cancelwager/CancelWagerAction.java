package com.nextgen.gameaggregator.vendor.hacksawgaming.api.cancelwager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.ResponseCodes;
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
public class CancelWagerAction {

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

    @PostMapping(path = EndPoints.CANCEL_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        try {

            CancelWagerDto dto = HttpService.convertJsonToDto(body, CancelWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Get last game session
            RawGameSession rawGameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getBrandUid());

            // Verify data
            this.doVerification(dto, rawGameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, rawGameSession);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(rawGameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(rawGameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);

            // Set BalanceDataWalletVo Object
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SUCCESS));
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

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
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SYSTEM_ERROR));
            responseVo.setData(null);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setCode(ResponseCodes.UNKNOWN);
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.UNKNOWN));
            responseVo.setData(null);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(CancelWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelWagerDto dto, RawGameSession rawGameSession)
            throws InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException {

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(rawGameSession, dto.getBrandUid());

        // Verify currency
        ValidationUtils.isEquals(rawGameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
