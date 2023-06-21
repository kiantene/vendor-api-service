package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.evoplay.api.authenticate.InitService;
import com.nextgen.gameaggregator.vendor.evoplay.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.evoplay.api.endround.WinService;
import com.nextgen.gameaggregator.vendor.evoplay.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(EndPoints.PATH)
public class ApiAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private InitService initService;
    @Autowired
    private BetService betService;
    @Autowired
    private WinService winService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;

    // Handle incoming API requests
    @PostMapping
    public ResponseVo handleApiCall(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();

        String traceId = httpRequestLog.getId();

        try {
            String body = httpRequestLog.getRequestBody();
            CallbackDto callbackDto = VendorService.convertBodyToDto(body, CallbackDto.class);
            GameSession gameSession = gameSessionService.verifyToken(callbackDto.getToken());

            String projId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJ_ID);
            String key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KEY);

            Optional.ofNullable(projId).orElseThrow(InvalidVendorLineException::new);
            Optional.ofNullable(key).orElseThrow(InvalidVendorLineException::new);

            callbackDto.setProject(projId);
            callbackDto.setVersion(Formats.CALLBACK_VERSION);

            switch (callbackDto.getName()) {
                case "init" -> {
                    responseVo = initService.init(callbackDto, gameSession, traceId, key);
                }
                case "bet" -> {
                    responseVo = betService.bet(callbackDto, gameSession, body, traceId, key);
                }
                case "win" -> {
                    responseVo = winService.win(callbackDto, gameSession, httpRequestLog, traceId, key);
                }
                case "refund" -> {
                    responseVo = refundService.refund(callbackDto, gameSession, traceId, key);
                }
                // If the header does not match any of the expected values, return an error response
                default -> {
                    throw new InvalidRequestException();
                }
            }

        } catch (Exception e) {
            ResponseDataVo responseDataVo = new ResponseDataVo();
            responseDataVo.setScope("internal");
            responseDataVo.setNo_refund(1);
            responseDataVo.setMessage(ResponseCodes.ERROR.message);
            responseVo.setResponseCode(ResponseCodes.ERROR);
            responseVo.setError(responseDataVo);

            httpService.logError(httpRequestLog, e);
        } finally {
            // End the HTTP request logging and return the ResponseVo object
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }
}
