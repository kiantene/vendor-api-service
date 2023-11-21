package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.accept.AcceptService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.settled.SettledService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class GeneralAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private BetService betService;
    @Autowired
    private AcceptService acceptService;
    @Autowired
    private SettledService settledService;

    @PostMapping(path = "/{agentcode}/wagering/usercode/{usercode}/request/{requestid}")
    public ResponseVo handleApiCall(@PathVariable String agentcode, @PathVariable String usercode, @PathVariable String requestid, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            String body = httpRequestLog.getRequestBody();
            ObjectMapper objectMapper = new ObjectMapper();
            ActionsDto dto = objectMapper.readValue(body, ActionsDto.class);

            // Create a set to store action name
            Set<String> uniqueActionNames = new HashSet<>();

            List<CompletableFuture<List<CommonVo>>> futures = dto.getActions().stream()
                    .filter(action -> uniqueActionNames.add(action.getName())) // Only consider unique action names
                    .map(action -> CompletableFuture.supplyAsync(() -> actionsSwitching(action.getName(), dto, httpRequestLog)))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join(); // Wait for all services to complete

            // Combine the results
            List<CommonVo> commonVos = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream) // Flatten the list of lists into a single list
                    .collect(Collectors.toList());

            responseVo = mergeResponses(commonVos);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setErrorCode(errorCode);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private List<CommonVo> actionsSwitching(String actionName, ActionsDto dto, HttpRequestLog httpRequestLog) {
        List<CommonVo> commonVos = new ArrayList<>();
    
        switch (actionName) {
            case "BETTED" -> commonVos.addAll(betService.bet(dto, httpRequestLog));
            case "ACCEPTED" -> commonVos.addAll(acceptService.accept(dto, httpRequestLog));
            case "SETTLED" -> commonVos.addAll(settledService.settled(dto, httpRequestLog));
            default -> {
                CommonVo commonVo = new CommonVo();
                commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
                commonVos.add(commonVo);
            }
        }
    
        return commonVos;
    }    

    private ResponseVo mergeResponses(List<CommonVo> commonVos) {
        ResponseVo responseVo = new ResponseVo();
        ResultVo result = new ResultVo();

        result.setActions(commonVos);
        responseVo.setResult(result);
        responseVo.setErrorCode(ResponseCode.SUCCESS.code);

        return responseVo;
    }
}

