package com.nextgen.gameaggregator.vendor.evolutionlive.api.check;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolutionlive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CheckAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = {EndPoints.CHECK, EndPoints.SID})
    public ResponseVo CheckAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String authToken = request.getParameter("authToken");
            Optional.ofNullable(authToken).orElseThrow(InvalidRequestException::new);
            String body = httpRequestLog.getRequestBody();
            CheckDto checkDto = HttpService.convertJsonToDto(body, CheckDto.class);


            // 1. Validate request parameters (Non-database calls)
//            this.doValidation(balanceDto, authToken);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(checkDto.getUserId());

//            this.doVerification(dto, gameSession);

            // 3. Retrieve the latest wallet balance from Operator
//            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            responseVo.setSid(gameSession.getToken());
            responseVo.setUuid(traceId);


        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);

        } finally {

        }
        return responseVo;
    }
}
