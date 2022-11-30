package com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.action;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.vendorapiservice.api.v1.apicomponent.log.TraceIdLog;
import com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.constant.RequestConstant;
import com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.dto.BetRequestActionDto;
import com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.vo.BetRequestActionVo;
import com.nextgen.gameaggregator.vendorapiservice.api.vendor.servicecomponent.seamless.SeamlessVendorAdaptor;
import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.*;
import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager.*;
import com.nextgen.gameaggregator.vendorapiservice.grpc.dto.VendorGameBetRequestServiceRequestDto;
import com.nextgen.sas.core.web.action.Action;
import com.nextgen.sas.core.web.action.WebActionRequest;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zipkin2.internal.Trace;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = RequestConstant.BET_REQUEST_ACTION)
public class BetRequestAction extends Action implements WebActionRequest {

    @Autowired
    private TraceIdLog traceIdLog;
    private BetRequestActionVo betRequestActionVo;

    @Autowired
    private VendorReaderManager vendorReaderManager;
    private VendorReader vendorReader;
    private SeamlessVendorAdaptor seamlessVendorAdaptor;

    private VendorGameBetRequestServiceRequestDto vendorGameBetRequestServiceRequestDto;


    public BetRequestAction(SeamlessVendorAdaptor seamlessVendorAdaptor) {
        this.seamlessVendorAdaptor = seamlessVendorAdaptor;
        this.betRequestActionVo = new BetRequestActionVo();
    }

    @PostMapping("")
    public ResponseEntity<BetRequestActionVo> action(@Valid @RequestBody BetRequestActionDto dto, HttpServletRequest request) {
        if (betRequestActionVo.error.getValidation().isEmpty()) {
            this.actionDtoToServiceDto(dto);

            //region get vendor_class file name
            vendorReader = vendorReaderManager.findByVendorCode(RequestConstant.VENDOR_CODE);
            //endregion

            //region call to correct class file
            seamlessVendorAdaptor.getVendor(vendorReader.getClassFile());

//            VendorGameBetRequestServiceRequestDto vendorGameBetRequestServiceRequestDtod = seamlessVendorAdaptor.seamlessVendor.betRequest(betRequestServiceDto);

        } else {
            betRequestActionVo.setStatus(false);
        }
        System.out.println("end " + Instant.now().toEpochMilli());
        return ResponseEntity.ok(betRequestActionVo);
    }

    @Override
    public String verify(WebRequestWrapper request) {
        var dto = new Gson().fromJson(getRequestJsonString(request), BetRequestActionDto.class);

        traceIdLog.storeRequest(UUID.randomUUID().toString(), dto.getUserId().toString(),
                this.getClass().getCanonicalName(), getRequestJsonString(request));
        //System.out.println(getRequestJsonString(request));

        Map<String, String> validationMap = new HashMap<String, String>();



        System.out.println("Test check verify 2");
        System.out.println("Test check verify 1111");
        return null;
    }

    private VendorGameBetRequestServiceRequestDto actionDtoToServiceDto(BetRequestActionDto betRequestActionDto) {
        vendorGameBetRequestServiceRequestDto = new VendorGameBetRequestServiceRequestDto();
        return vendorGameBetRequestServiceRequestDto;
    }
}
