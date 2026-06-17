package com.nextgen.gameaggregator.vendor.topbet.api.authenticate;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateController {

    @PostMapping(path = EndPoints.HEALTH)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<SuccessResponse> authenticate(
            @Valid @RequestBody AuthenticateRequest request) {
        SuccessResponse response = SuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .build();
        return ResponseEntity.ok(response);
    }
}
