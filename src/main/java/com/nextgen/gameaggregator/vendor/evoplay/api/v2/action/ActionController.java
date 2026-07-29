package com.nextgen.gameaggregator.vendor.evoplay.api.v2.action;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DetailsDto;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ActionName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.balanceIncrease.BalanceIncreaseService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.bet.BetServices;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.result.BetResultService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;

import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping(path = EndPoints.PATH + "/v2")
public class ActionController {
    private final BetServices betServices;
    private final BetResultService betResultService;
    private final RollbackService rollbackService;
    private final HttpService httpService;
    private final BalanceService balanceService;
    private final BalanceIncreaseService balanceIncreaseService;
    private final ObjectMapper objectMapper;

    public ActionController(
            BetServices betServices,
            BetResultService betResultService,
            RollbackService rollbackService,
            HttpService httpService,
            BalanceService balanceService,
            BalanceIncreaseService balanceIncreaseService,
            ObjectMapper objectMapper) {
        this.betServices = betServices;
        this.betResultService = betResultService;
        this.rollbackService = rollbackService;
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.balanceIncreaseService = balanceIncreaseService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<ResponseVo> generalAction(HttpServletRequest httpRequest, @RequestParam Map<String, String> formFields) {
        HttpRequestLog httpRequestLog = httpService.start(httpRequest);
        String traceId = httpRequestLog.getId();
        ResponseVo response = new ResponseVo();

        String action = formFields.get("name");

        CallbackDto callbackDto = convertAndValidate(formFields);
        if (callbackDto.getData() != null && callbackDto.getData().getDetails() != null) {
            DetailsDto detailsDto = new Gson().fromJson(
                    callbackDto.getData().getDetails(),
                    DetailsDto.class
            );
            callbackDto.getData().setDetailsDto(detailsDto);
        }
        return switch (ActionName.valueOf(action.toLowerCase())) {
            case init -> balanceService.getBalance(callbackDto);
            case bet -> betServices.bet(callbackDto);
            case win -> betResultService.result(callbackDto);
            case refund -> rollbackService.rollback(callbackDto);
            case balanceincrease -> {
                response = balanceIncreaseService.balanceIncrease(callbackDto, traceId, httpRequestLog);
                yield ResponseEntity.ok(response);
            }
            default -> {
                response.setResponseCode(ResponseCodes.INVALID_REQUEST_ERROR);
                yield ResponseEntity.ok(response);
            }
        };
    }

    private CallbackDto convertAndValidate(Map<String, String> source) {
        Map<String, Object> nestedMap = new HashMap<>();

        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith("data[")) {
                String nestedKey = key.substring(key.indexOf("[") + 1, key.indexOf("]"));

                @SuppressWarnings("unchecked")
                Map<String, Object> dataNode = (Map<String, Object>) nestedMap.computeIfAbsent("data", k -> new HashMap<String, Object>());

                dataNode.put(nestedKey, entry.getValue());
            } else {
                nestedMap.put(key, entry.getValue());
            }
        }
        return objectMapper.convertValue(nestedMap, CallbackDto.class);
    }
}
