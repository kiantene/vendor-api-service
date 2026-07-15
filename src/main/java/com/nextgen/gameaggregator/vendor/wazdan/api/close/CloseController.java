package com.nextgen.gameaggregator.vendor.wazdan.api.close;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.vendor.wazdan.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.wazdan.constant.ResponseCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CloseController {
    @PostMapping(path = EndPoints.CLOSE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CloseResponse> close(
            @Valid @RequestBody CloseRequest request) {
        return ResponseEntity.ok(CloseResponse.builder()
                .status(ResponseCode.SUCCESS.code)
                .build());
    }
}
