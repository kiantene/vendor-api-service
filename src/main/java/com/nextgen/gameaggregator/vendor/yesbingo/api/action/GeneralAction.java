package com.nextgen.gameaggregator.vendor.yesbingo.api.action;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.api.balance.BalanceAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.bet.BetAction;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Actions;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private BalanceAction balanceAction;
    @Autowired
    private BetAction betAction;

    @PostMapping(path = EndPoints.PATH + "/{id}")
    public ResponseVo balance(HttpServletRequest request, @PathVariable String id) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            //Get vendor line id by agent code from vendor line credential
            // Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue();


            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            VendorRequestDto vendorRequestDto = HttpService.convertQueryStringToDto(body, VendorRequestDto.class);

            // Validate request parameters (Non-database related)
            ValidationUtils.validateRequest(vendorRequestDto);

            // Get the first vendor line id from list
            Integer vendorLineId = vendorLineService.getVendorLineIdListByNameAndValue(Credentials.YESBINGO_ID, id);

            // Get the key and iv value with vendorLineId
            String key = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AES_KEY);
            String iv = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AES_IV);

            ActionDto dto = HttpService.convertJsonToDto(body, ActionDto.class);



            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            switch (dto.getAction()) {
                case Actions.BALANCE -> {
                    responseVo = balanceAction.balance(httpRequestLog, traceId, body);
                }
                case Actions.BET -> {
                    responseVo = betAction.bet(httpRequestLog, traceId, body);
                }
                // If the header does not match any of the expected values, return an error response
                default -> {
                    throw new InvalidRequestException();
                }
            }

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setStatus(ResponseCodes.UNKNOWN_ACTION);
        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

}
