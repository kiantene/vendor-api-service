package com.nextgen.gameaggregator.vendor.kypoker.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.kypoker.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.kypoker.api.settle.SettleService;
import com.nextgen.gameaggregator.vendor.kypoker.api.cancel.CancelService;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Actions;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
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
    private final HttpService httpService;
    private final BalanceService balanceService;
    private final BetService betService;
    private final SettleService settleService;
    private final CancelService cancelService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;


    public GeneralAction(HttpService httpService,
                         BalanceService balanceService,
                         BetService betService,
                         SettleService settleService, CancelService cancelService,
                         VendorLineService vendorLineService,
                         ValidationService validationService,
                         GameSessionService gameSessionService) {
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.betService = betService;
        this.settleService = settleService;
        this.cancelService = cancelService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
    }

    @PostMapping(path = "/{id}" + EndPoints.LAUNCH_GAME )
    public CommonVo action(HttpServletRequest request, @PathVariable String id) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo vo = new CommonVo();


        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            CommonDto commonDto = HttpService.convertQueryStringToDto(body, CommonDto.class);

            // Validate request parameters (Non-database related)
            ValidationUtils.validateRequest(commonDto);

            Integer vendorLineId = vendorLineService.getVendorLineIdListByNameAndValue(Credentials.KYP_ID, id);

            String aesKey = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AES_KEY);

            String md5Key = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.MD5_KEY);

            String decryptedBody = VendorService.AESDecrypt(body, aesKey, true);

            String encryptedMd5 = VendorService.MD5Encrypt(commonDto.getAgent()+commonDto.getTimestamp()+md5Key);

            doVerification(commonDto,encryptedMd5);

            ActionDto actionDto = HttpService.convertQueryStringToDto(decryptedBody, ActionDto.class);

            // Handle the action and return the resulting value
            vo = this.actionHandling(body, traceId, httpRequestLog, actionDto);

        } catch (Exception e) {
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.INTERNAL_ERROR);
        }
        finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doVerification(CommonDto dto,String encryptedMd5) throws InvalidRequestException {
        ValidationUtils.isEquals(dto.getKey(), encryptedMd5);

    }

    private CommonVo actionHandling(String body, String traceId, HttpRequestLog httpRequestLog, ActionDto actionDto) throws AuthenticationException {
        CommonVo vo = new CommonVo();
        switch (actionDto.getS()) {
            case Actions.BALANCE -> vo = balanceService.balance(body, traceId, httpRequestLog);
            case Actions.BET -> vo = betService.bet(body, traceId, httpRequestLog);
            case Actions.SETTLE -> vo = settleService.settle(body, traceId, httpRequestLog);
            case Actions.CANCEL -> vo = cancelService.cancel(body, traceId, httpRequestLog);

            default -> vo.sets(ResponseCodes.INTERNAL_ERROR);
        }

        return vo;
    }


}