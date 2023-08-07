package com.nextgen.gameaggregator.vendor.booongo.api.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.booongo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.booongo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nextgen.gameaggregator.vendor.booongo.service.VendorService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Service
@Slf4j
public class RollbackService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public CommonVo rollback(HttpRequestLog httpRequestLog, String traceId) {

        RollbackDto rollbackDto = new RollbackDto();

        // Construct vo
        RollbackVo vo = new RollbackVo();
        BalanceVo balanceVo = new BalanceVo();
        ErrorVo error = new ErrorVo();

        BigDecimal balance = null;

        GameSession gameSession = new GameSession();

        long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

        try{
            // Retrieve request body in original string format
            rollbackDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), RollbackDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(rollbackDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService);

            // Construct response data into vo
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            balanceVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setBalance(balanceVo);

        } catch (InvalidAgentApiCredentialException |
                 RecordNotFoundException |
                 AuthenticationException |
                 InvalidOperatorResponseException |
                 JsonProcessingException |
                 InvalidPlayerException |
                 CurrencyNotSupportedException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 DisabledVendorLineException |
                 CredentialNotFoundException e) {

            // vendor did not provide any error code, so using back general transaction error
            error.setCode(ResponseCodes.OTHER_EXCEED);
            vo.setError(error);
        }catch(BetNotFoundException |
               BetRefundIdempotentViolationException | BetResultIdempotentViolationException e){
            balance = getCurrentBalance(traceId, gameSession);

            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            balanceVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setBalance(balanceVo);
        }catch(Exception exception){
            httpService.logError(httpRequestLog, exception);
            // vendor did not provide any error code, so using back general transaction error
            error.setCode(ResponseCodes.OTHER_EXCEED);
            vo.setError(error);
        }
         finally{
            vo.setUid(rollbackDto.getUid());
        }

        return vo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getArgs());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getArgs().getPlayer());
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws InvalidPlayerException, InvalidRequestException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException, CurrencyNotSupportedException, CredentialNotFoundException {

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getArgs().getPlayer().getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getArgs().getPlayer().getCurrency(), CurrencyNotSupportedException::new);

        //Verify received brand is same with credential
        String brand = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJECT_NAME);
        ValidationUtils.isEquals(brand, dto.getArgs().getPlayer().getBrand(), InvalidRequestException::new);
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession) {

        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession);

        } catch (Exception exception) {

        }

        return balance;
    }
}
