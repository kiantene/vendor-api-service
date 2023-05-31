package com.nextgen.gameaggregator.vendor.bng.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.bng.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Service
@Slf4j
public class BalanceService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {

        BalanceDto balanceDto = new BalanceDto();

        // Construct VO
        BalanceServiceVo vo = new BalanceServiceVo();
        BalanceVo balanceVo = new BalanceVo();
        ErrorVo error = new ErrorVo();

        try {
            // Retrieve request body in original string format
            balanceDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), BalanceDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(balanceDto.getToken());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

            // Construct response data into vo
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            balanceVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setBalance(balanceVo);

        } catch (InvalidAgentApiCredentialException |
                 AuthenticationException |
                 InvalidOperatorResponseException |
                 JsonProcessingException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 DisabledVendorLineException |
                 CurrencyNotSupportedException e) {
            error.setCode("GAME_NOT_ALLOWED");
            vo.setError(error);
        } finally{
            vo.setUid(balanceDto.getUid());
        }

        return vo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws InvalidPlayerException, InvalidRequestException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException, CurrencyNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getArgs().getPlayer().getId());

        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getArgs().getPlayer().getCurrency(), CurrencyNotSupportedException::new);
    }
}
