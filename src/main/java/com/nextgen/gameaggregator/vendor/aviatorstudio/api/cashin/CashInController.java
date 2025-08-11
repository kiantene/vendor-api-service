package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.rollback.RollbackService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.settle.SettleService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ReasonCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class CashInController {
    private final SettleService settleService;
    private final RollbackService rollbackService;

    @PostMapping(path = EndPoints.CASHIN)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CashInResponse> doCashIn(
            @Valid @RequestBody CashInRequest request,
            @RequestAttribute("token") String token,
            @RequestAttribute("username") String username) {

        return isSettle(request)
                ? settleService.doSettle(request, token, username)
                : rollbackService.doRollback(request, token, username);
    }

    private boolean isSettle(CashInRequest request) {
        return !ReasonCode.isRefundReason(request.getReason());
    }
}
