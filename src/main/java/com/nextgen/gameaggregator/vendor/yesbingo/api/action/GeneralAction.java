package com.nextgen.gameaggregator.vendor.yesbingo.api.action;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.api.balance.BalanceAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.bet.BetAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.result.GameResultAction;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Actions;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.dto.VendorRequestDto;
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
    @Autowired
    private GameResultAction gameResultAction;

    @PostMapping(path = EndPoints.ACTION + "/{id}")
    public ResponseVo balance(HttpServletRequest request, @PathVariable String id) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {

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

            // Decrypt data
            String decryptedData = VendorService.decrypt(vendorRequestDto.getX(), key, iv);

            // Update the requestBody for easier debugging purposes
            httpRequestLog.setRequestBody(body + "&json=" + decryptedData);

            // Map the decrypted data
            ActionDto dto = HttpService.convertJsonToDto(decryptedData, ActionDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            switch (dto.getAction()) {
                case Actions.BALANCE -> {
                    responseVo = balanceAction.balance(httpRequestLog, traceId, decryptedData);
                }
                case Actions.BET -> {
                    responseVo = betAction.bet(httpRequestLog, traceId, decryptedData);
                }
                case Actions.GAME_RESULT -> {
                    responseVo = gameResultAction.gameResult(httpRequestLog, traceId, decryptedData);
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
