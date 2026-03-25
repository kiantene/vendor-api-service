package com.nextgen.gameaggregator.vendor.cockfight6.api.action;

import com.nextgen.gameaggregator.vendor.cockfight6.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.cockfight6.api.result.ResultService;
import com.nextgen.gameaggregator.vendor.cockfight6.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nextgen.gameaggregator.vendor.cockfight6.constant.RequestType.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class ActionController {
    private final BetService betService;
    private final ResultService resultService;
    private final RollbackService rollbackService;

    public ActionController(BetService betService, ResultService resultService, RollbackService rollbackService) {
        this.betService = betService;
        this.resultService = resultService;
        this.rollbackService = rollbackService;
    }

    @PostMapping(path = EndPoints.ACTION)
    public ResponseEntity<CommonSuccessResponse> generalAction(HttpServletRequest httpRequest, @Valid @RequestBody CommonRequest request) {

        switch (request.getReqType()) {
            case REQ_TYPE_BET:
                return betService.bet(request);
            case REQ_TYPE_SETTLE:
                return resultService.result(request);
            case REQ_TYPE_ROLLBACK:
                return rollbackService.rollback(request);
            default:
                return ResponseEntity.badRequest().body(
                        CommonSuccessResponse.builder()
                                .code(ResponseCode.INVALID_REQUEST.code)
                                .msg(ResponseCode.INVALID_REQUEST.message)
                                .build()
                );
        }
    }
}
