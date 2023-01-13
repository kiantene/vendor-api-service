package com.nextgen.gameaggregator.vendor.cq9.api.balance;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;

    @GetMapping(path = EndPoints.BALANCE)
    public ResponseVo<CommonVo> balance(@PathVariable("account") String account, HttpServletRequest request, @RequestHeader(value = "wtoken") String wToken) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        try {
            CommonVo commonVo = new CommonVo();

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(account);

            // 2. Get vendor player details
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(account);

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(vendorPlayer, wToken);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, account);

            // 5. Get vendor line supported currency
            VendorLine vendorLine = vendorLineService.getVendorLineById(vendorPlayer.getVendorLineId());

            commonVo.setBalance(balance);
            commonVo.setCurrency(vendorLine.getVendorCurrencyCode());

            responseVo.setData(commonVo);

        } catch (CredentialNotFoundException credentialNotFoundException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidPlayerException invalidPlayerException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

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

    private void doValidation(String username) throws InvalidPlayerException {
        // Validation with custom exception
        ValidationUtils.validateLength(username, 3, 20, InvalidPlayerException::new);
    }

    private void doVerification(VendorPlayer vendorPlayer, String wToken) throws InvalidPlayerException, InvalidVendorLineException, CredentialNotFoundException {
        // 2. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(vendorPlayer.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 3. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }
}
