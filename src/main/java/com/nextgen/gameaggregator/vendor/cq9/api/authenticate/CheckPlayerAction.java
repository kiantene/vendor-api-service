package com.nextgen.gameaggregator.vendor.cq9.api.authenticate;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CheckPlayerAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @GetMapping(path = EndPoints.AUTHENTICATE)
    public ResponseVo<Boolean> authenticate(HttpServletRequest request, @PathVariable String account) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String wToken = request.getHeader("wtoken");
        CheckPlayerPathVariableDto pathVariableDto = new CheckPlayerPathVariableDto();
        pathVariableDto.setAccount(account);

        // Construct Vo
        ResponseVo<Boolean> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);
        responseVo.setData(false);

        try {
            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(pathVariableDto, wToken);

            // 2. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, pathVariableDto.getAccount(), wToken);

            responseVo.setData(true);

        } catch (CredentialNotFoundException credentialNotFoundException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidPlayerException invalidPlayerException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidRequestException invalidRequestException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

        } catch (InvalidVendorLineException invalidVendorLineException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CheckPlayerPathVariableDto pathVariableDto, String wToken) throws InvalidPlayerException, InvalidRequestException{
        // Validate value from Header and Path Variable
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);
        Optional.ofNullable(pathVariableDto.getAccount()).orElseThrow(InvalidRequestException::new);

        // Validation with custom exception
        ValidationUtils.validateRequest(pathVariableDto);
        ValidationUtils.validateLength(pathVariableDto.getAccount(), 3, 20, InvalidPlayerException::new);
    }

    private void doVerification(HttpRequestLog request, String username, String wToken) throws InvalidPlayerException, InvalidVendorLineException, CredentialNotFoundException {
        // 1. Check is player account exists
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(username);

        // 2. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(vendorPlayer.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 3. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }
}
