package com.nextgen.gameaggregator.vendor.dblive.api.batchbalance;

import com.fasterxml.jackson.core.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.dblive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dblive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dblive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dblive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.dblive.service.VendorService;
import com.nextgen.gameaggregator.vendor.dblive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.nextgen.gameaggregator.vendor.dblive.service.VendorService.convertDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BatchBalanceAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    public BatchBalanceAction(HttpService httpService, VendorLineService vendorLineService,
                              GameSessionService gameSessionService, WalletService walletService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.BATCH_BALANCE)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        try {
            String body = httpRequestLog.getRequestBody();

            //validate request param
            CommonDto batchBalanceDto = HttpService.convertJsonToDto(body, CommonDto.class);
            VendorService.doValidation(batchBalanceDto);

            BatchParamDto batchParamsDto = VendorService.convertDto(batchBalanceDto.getParams(), BatchParamDto.class);
            VendorService.doValidation(batchParamsDto);

            //Query list of user balance
            List<BatchBalanceDataVo> balanceDataList = getBatchBalanceDataVos(batchParamsDto, traceId, httpRequestLog);

            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(batchParamsDto.getLoginNames().get(0));
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername
                    (vendorPlayerUsername);

            //Verification
            this.doVerification(batchBalanceDto, gameSession);
            String md5Key = vendorLineService.getCredentialValueByName
                    (gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            String signature = VendorService.getMD5(balanceDataList, md5Key);
            responseVo.setResponseSuccess(balanceDataList, signature);
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCodes.INVALID_PLAYER_SESSION);
        } catch (CredentialNotFoundException | JsonParseException |
                 UnexpectedTypeException | InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
        } catch (InvalidSignatureException exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.OTHER_ERROR);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    @NotNull
    private List<BatchBalanceDataVo> getBatchBalanceDataVos(BatchParamDto batchParamsDto, String traceId, HttpRequestLog httpRequestLog) {
        List<BatchBalanceDataVo> balanceDataList = new ArrayList<>();

        List<String> usernames = batchParamsDto.getLoginNames();
        usernames.forEach(username -> {
            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(username);
            try {
                GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayerUsername);

                // Get walletBalance
                BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

                //Encrypt Data
                BatchBalanceDataVo balanceData = new BatchBalanceDataVo();
                balanceData.setLoginName(username);
                balanceData.setBalance(convertDecimal(balance));

                balanceDataList.add(balanceData);
            } catch (AuthenticationException | InvalidAgentApiCredentialException |
                     VendorCurrencyNotSupportException | InvalidOperatorResponseException e) {

                //If catched exception, set balance to 0
                BatchBalanceDataVo balanceData = new BatchBalanceDataVo();
                balanceData.setLoginName(username);
                balanceData.setBalance(BigDecimal.ZERO);

                balanceDataList.add(balanceData);
            }
        });
        return balanceDataList;
    }

    private void doVerification(CommonDto batchBalanceDto, GameSession gameSession) throws CredentialNotFoundException, InvalidSignatureException {

        //Verify Signature is match
        String md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);
        VendorService.verifySignature(batchBalanceDto.getParams(), md5Key, batchBalanceDto.getSignature());
    }
}
