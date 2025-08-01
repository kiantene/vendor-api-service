package com.nextgen.gameaggregator.vendor.inout.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.inout.constant.Actions;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;

    public GeneralAction(HttpService httpService,
                         VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String xSign = request.getHeader("X-REQUEST-SIGN");
        String secretKey;
        Integer vendorLineId;

        CommonVo vo = new CommonVo();

        try{
            String body = httpRequestLog.getRequestBody();

            CommonDto dto = HttpService.convertJsonToDto(body, CommonDto.class);

            ValidationUtils.validateRequest(dto);

            vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.OPERATOR_ID, dto.getCommonDataDto().getOperator());

            secretKey = vendorLineService.getCredentialValueByName(vendorLineId,Credentials.SECRET_KEY);

            this.doVerification(body,secretKey,xSign);

            vo = this.actionHandling(body,dto);

        }  catch (InvalidRequestException e){
            vo.setCodeMessages(String.valueOf(ResponseCode.INVALID_TOKEN));

        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return vo;
    }

    private void doVerification(String body, String secretKey, String xSign) throws InvalidRequestException {

        String requestBody = VendorService.hashHMACSha256(body, secretKey);
        ValidationUtils.isEquals(xSign, requestBody);
    }

    private CommonVo actionHandling(String body, CommonDto commonDto){
        CommonVo vo = new CommonVo();

        switch (commonDto.getAction()) {

            case Actions.INITIALIZE:
                break;

            case Actions.BET:
                break;

            case Actions.WITHDRAW:
                break;

            case Actions.ROLLBACK:
                break;

            default:
                break;

        }
            return  vo;

    }
}
