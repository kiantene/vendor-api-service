package com.nextgen.gameaggregator.vendor.spadegaming.api;

import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Headers;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.dto.AuthenticateDto;
import com.nextgen.gameaggregator.vendor.spadegaming.dto.BalanceDto;
import com.nextgen.gameaggregator.vendor.spadegaming.dto.TransferDto;
import com.nextgen.gameaggregator.vendor.spadegaming.service.AuthenticateService;
import com.nextgen.gameaggregator.vendor.spadegaming.service.BalanceService;
import com.nextgen.gameaggregator.vendor.spadegaming.service.TransferService;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.ResponseVo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoints.PATH)
public class ApiAction {
    private final AuthenticateService authenticateService;
    private final BalanceService balanceService;
    private final TransferService transferService;

    @Autowired
    public ApiAction(AuthenticateService authenticateService,
                     BalanceService balanceService,
                     TransferService transferService) {
        this.authenticateService = authenticateService;
        this.balanceService = balanceService;
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseVo handleApiCall(@RequestHeader(Headers.HEADER_KEY_API) String apiAction,
            @RequestBody(required = false) AuthenticateDto authenticateDto,
            @RequestBody(required = false) BalanceDto balanceDto,
            @RequestBody(required = false) TransferDto transferDto) {

            switch (apiAction) {
                case Headers.HEADER_VALUE_AUTHENTICATE:
                    return authenticateService.authenticate(authenticateDto);

                case Headers.HEADER_VALUE_BALANCE:
                    return balanceService.balance(balanceDto);

                case Headers.HEADER_VALUE_TRANSFER:
                    return transferService.transfer(transferDto);

                default:
                    ResponseVo responseVo = new ResponseVo();
                    responseVo.setMerchantCode(Credentials.MERCHANT_CODE);
                    responseVo.setMsg(ResponseCode.RESPONSE_DESCRIPTION.get(ResponseCode.INVALID_REQUEST));
                    responseVo.setCode(ResponseCode.INVALID_REQUEST);
                    responseVo.setSerialNo("");
                    return responseVo;
            }
    }

}
