package com.nextgen.gameaggregator.vendor.smartsoft.action;

import com.nextgen.gameaggregator.vendor.wmlive.constant.EndPoints;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {
//
//    private final HttpService httpService;
//    private final BalanceService balanceService;
//    private final PointInOutService pointInOutService;
//    private final TimeoutBetReturnService timeoutBetReturnService;
//
//    @Autowired
//    public GeneralAction(HttpService httpService,
//                         BalanceService balanceService, PointInOutService pointInOutService,
//                         TimeoutBetReturnService timeoutBetReturnService) {
//        this.httpService = httpService;
//        this.balanceService = balanceService;
//        this.pointInOutService = pointInOutService;
//        this.timeoutBetReturnService = timeoutBetReturnService;
//
//    }
//
//    //Handle incoming API requests
//    @PostMapping(path = EndPoints.ACTION)
//    public ResponseVo handleApiCall(HttpServletRequest request) {
//        // Start the HTTP request logging
//        HttpRequestLog httpRequestLog = httpService.start(request);
//
//        String traceId = httpRequestLog.getId();
//
//        ResponseVo responseVo = new ResponseVo();
//        try {
//            // Retrieve request body in original string format
//            String body = httpRequestLog.getRequestBody();
//
//            GeneralActionDto dto = HttpService.convertQueryStringToDtoUrlDecode(body, GeneralActionDto.class);
//
//            responseVo = commandsSwitching(dto, traceId, httpRequestLog);
//
//        } catch (Exception e) {
//            httpService.logError(httpRequestLog, e);
//            responseVo.setResponseCodeMsg(ResponseCode.ERROR);
//
//        } finally {
//            httpService.end(httpRequestLog, responseVo);
//        }
//        return responseVo;
//    }
//
//    private ResponseVo commandsSwitching(GeneralActionDto dto, String traceId, HttpRequestLog httpRequestLog) {
//        ResponseVo commandSwitchingVo = new ResponseVo();
//        String cmd = dto.getCmd();
//        switch (cmd) {
//            case Commands.CALL_BALANCE -> commandSwitchingVo = balanceService.getBalance(traceId, httpRequestLog);
//            case Commands.POINT_IN_OUT ->
//                    commandSwitchingVo = pointInOutService.pointInOut(dto, traceId, httpRequestLog);
//            case Commands.ROLL_BACK ->
//                    commandSwitchingVo = timeoutBetReturnService.timeoutBetReturn(dto, httpRequestLog);
//            default -> commandSwitchingVo.setResponseCodeMsg(ResponseCode.ERROR);
//
//        }
//        return commandSwitchingVo;
//    }

}
