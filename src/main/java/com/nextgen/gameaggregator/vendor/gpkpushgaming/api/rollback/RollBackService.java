package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;

@Service
@Slf4j
public class RollBackService {
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;

    @Autowired
    public RollBackService(GameSessionService gameSessionService,
                           VendorLineService vendorLineService,
                           WalletService walletService,
                           HttpService httpService,
                           VendorService vendorService) {
        this.gameSessionService = gameSessionService;

        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
    }

    public CommonVo rollback(HttpRequestLog httpRequestLog, String traceId) {
        RollBackDto rollBackDto = new RollBackDto();
        CommonVo vo = new CommonVo();
        RollBackDataVo dataVo = new RollBackDataVo();

        BigDecimal balance = BigDecimal.ZERO;

        GameSession gameSession = null;

        try {
            // Retrieve request body in original string format
            rollBackDto = HttpService.convertQueryStringToDto(URLDecoder.decode(httpRequestLog.getRequestBody(), "UTF-8"), RollBackDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollBackDto);

            // Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(rollBackDto.getUser());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollBackDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, rollBackDto, gameSession, vendorService, httpRequestLog);

            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            dataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
            dataVo.setMoney(rollBackDto.getMoney().setScale(2, RoundingMode.DOWN));
            dataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            dataVo.setDealid(rollBackDto.getDealid());

            vo.setData(dataVo);
        } catch (BetResultIdempotentViolationException e) {
            // vendor site already thread this transaction as cancel no matter we return error or success
            httpService.logError(httpRequestLog, e);

            balance = e.getBalance();
            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            dataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
            dataVo.setMoney(rollBackDto.getMoney());
            dataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            dataVo.setDealid(rollBackDto.getDealid());

            vo.setData(dataVo);

        } catch (BetNotFoundException | BetRefundIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR.code);

//            vo.setCodeMsg(ResponseCodes.SUCCESS.code);
//
//            dataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
//            dataVo.setMoney(rollBackDto.getMoney());
//            dataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
//            dataVo.setDealid(rollBackDto.getDealid());
//
//            vo.setData(dataVo);

        } catch (Exception e) {
            // this error code is for trigger retry(vendor will thread this transaction as cancel)
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR.code);
        }

        return vo;
    }

    private void doValidation(RollBackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollBackDto dto, GameSession gameSession) throws InvalidPlayerException, CredentialNotFoundException, InvalidRequestException {
        //Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser(), InvalidPlayerException::new);

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_TOKEN);
        ValidationUtils.isEquals(token, dto.getApiToken(), InvalidRequestException::new);

        // check platform id
        if (!PlatformType.getPlatformTypeList().contains(dto.getPlatform())) {
            throw new InvalidRequestException();
        }
    }

}
