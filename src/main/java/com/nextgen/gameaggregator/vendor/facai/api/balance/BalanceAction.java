package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {


    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping(path = EndPoints.BALANCE)
    public BalanceVo balance(HttpServletRequest request) throws InvalidRequestException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        BalanceVo balanceVo = new BalanceVo();
        balanceVo.setResult(0);
        balanceVo.setMainPoints(new BigDecimal(1000.00));

//        try {
//            // Retrieve request body in original string format
//            String body = httpRequestLog.getRequestBody();
//
//            // Convert original request body into dto
//            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
//
//            //credential key = Q7RaR8CUbwZ0roD2
//            //Decrypt raw respond
//            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), "Q7RaR8CUbwZ0roD2");
//            System.out.println(jsonParam);
//
//            //map decrypted data(string json) into balanceDto
//            ObjectMapper objectMapper = new ObjectMapper();
//            BalanceDto balanceDto = objectMapper.readValue(jsonParam, BalanceDto.class);
//            System.out.println(balanceDto.Currency);
//
//            // 2. Get vendor player details
//            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getMemberAccount());
//
//            // 4. Get walletBalance
//            BigDecimal balance = walletService.getBalance(traceId, gameSession);
//
//            balanceVo.setResult(ResponseCodes.SUCCESS);
//            balanceVo.setMainPoints(balance);
//
//        } catch (Exception exception) { // any other exception encountered
//            balanceVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
//            balanceVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
//            httpService.logError(httpRequestLog, exception);
//
//        } finally {
//            httpService.end(httpRequestLog, balanceVo);
//        }


        httpService.end(httpRequestLog, balanceVo);

        return balanceVo;

    }

}
