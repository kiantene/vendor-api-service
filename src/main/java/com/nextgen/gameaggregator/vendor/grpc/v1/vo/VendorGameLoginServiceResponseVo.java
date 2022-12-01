package com.nextgen.gameaggregator.vendor.grpc.v1.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorGameLoginServiceResponseVo {
    private Boolean status;
    private String gameUrl;
    private String vendorErrorCode;
    private String vendorErrorMessage;

}
