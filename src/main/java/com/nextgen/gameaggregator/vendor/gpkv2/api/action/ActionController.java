package com.nextgen.gameaggregator.vendor.gpkv2.api.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.vendor.gpkv2.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.gpkv2.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.gpkv2.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.gpkv2.api.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.gpkv2.api.dto.VendorGameRequest;
import com.nextgen.gameaggregator.vendor.gpkv2.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.gpkv2.api.result.BetResultService;
import com.nextgen.gameaggregator.vendor.gpkv2.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.gpkv2.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class ActionController {
    private final BalanceService balanceService;
    private final BetService betService;
    private final BetResultService betResultService;
    private final RollbackService rollbackService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ActionController(BalanceService balanceService, BetService betService, BetResultService betResultService, RollbackService rollbackService, ObjectMapper objectMapper, Validator validator) {
        this.balanceService = balanceService;
        this.betService = betService;
        this.betResultService = betResultService;
        this.rollbackService = rollbackService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping(path = EndPoints.ACTION)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> generalAction(HttpServletRequest httpRequest,
                                                  @RequestBody VendorGameRequest rawbody) {

        switch (rawbody.getAction()) {
            case "balance": {
                CommonDto commonDto = convertAndValidate(rawbody, CommonDto.class);
                return balanceService.getBalance(commonDto);
            }
            case "bet": {
                BetRequest betRequest = convertAndValidate(rawbody, BetRequest.class);
                return betService.bet(betRequest);
            }
            case "win": {
                BetResultRequest betResultRequest = convertAndValidate(rawbody, BetResultRequest.class);
                return betResultService.result(betResultRequest);

            }
            case "cancel": {
                RollbackRequest rollbackRequest = convertAndValidate(rawbody, RollbackRequest.class);
                return rollbackService.rollback(rollbackRequest);
            }
            default:
                CommonVo commonVo = new CommonVo();
                commonVo.setErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
                return ResponseEntity.ok(commonVo);
        }
    }

    private <T> T convertAndValidate(Object source, Class<T> targetClass) {
        T obj = objectMapper.convertValue(source, targetClass);
        Set<ConstraintViolation<T>> violations = validator.validate(obj);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return obj;
    }
}
