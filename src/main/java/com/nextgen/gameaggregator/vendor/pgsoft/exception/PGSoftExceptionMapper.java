package com.nextgen.gameaggregator.vendor.pgsoft.exception;

import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutVo;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;

public class PGSoftExceptionMapper implements VendorExceptionMapper<ResponseVo<CashTransferInOutVo>> {

    @Override
    public String getVendorClassName() {
//        return Endpoints.CLASS_NAME;
        return null;
    }

    @Override
    public ResponseVo<CashTransferInOutVo> onInternalConfigurationError(InternalConfigurationException ex) {

        return null;
    }
}
