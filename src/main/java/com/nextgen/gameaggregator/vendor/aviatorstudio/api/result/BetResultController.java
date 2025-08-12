package com.nextgen.gameaggregator.vendor.aviatorstudio.api.result;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ReasonCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class BetResultController {
    private final BetResultService betResultService;
    private final RollbackService rollbackService;

    @PostMapping(path = EndPoints.CASHIN)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResultResponse> doBetResult(
            @Valid @RequestBody BetResultRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        return isSettle(request)
                ? betResultService.doSettle(request, token, username)
                : rollbackService.doRollback(request, token, username);
    }

    private boolean isSettle(BetResultRequest request) {
        return ReasonCode.isSettleReason(request.getReason());
    }
}
