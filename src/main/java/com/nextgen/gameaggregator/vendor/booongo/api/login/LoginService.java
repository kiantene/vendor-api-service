package com.nextgen.gameaggregator.vendor.booongo.api.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.booongo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.booongo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

@Service
@Slf4j
public class LoginService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public CommonVo login(HttpRequestLog httpRequestLog, String traceId) {

        LoginDto loginDto = new LoginDto();

        // Construct VO
        LoginVo vo = new LoginVo();
        LoginPlayerVo loginPlayer = new LoginPlayerVo();
        BalanceVo balanceVo = new BalanceVo();
        ErrorVo error = new ErrorVo();

        try {

            // Retrieve request body in original string format
            loginDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), LoginDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(loginDto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(loginDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(loginDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Retrieve vendor line credentials
            String brand = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJECT_NAME);

            // Construct response data into vo
            loginPlayer.setId(gameSession.getVendorPlayerUsername());
            loginPlayer.setBrand(brand);
            loginPlayer.setCurrency(gameSession.getVendorCurrencyCode());
            loginPlayer.setMode("REAL"); // "FUN" or "REAL", REAL by default. Mode of the player. 'FUN' stands for playing for fun not using real funds, 'REAL' stands for playing using real funds
            loginPlayer.setIs_test(false); // 'false' meant players are a subject for invoicing at production environment!

            // check stg vendor user name(testing acc from vendor)
            if(gameSession.getVendorPlayerUsername().equalsIgnoreCase("2z2kpq8zi") || gameSession.getVendorPlayerUsername().equalsIgnoreCase("2z2ksnosi")){
                loginPlayer.setIs_test(true); //if this param value is not same as previous login, it will cause unable to login game
            }

            long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond

            balanceVo.setValue(balance.toString());
            balanceVo.setVersion(BigInteger.valueOf(unixTime));

            vo.setPlayer(loginPlayer);
            vo.setBalance(balanceVo);
            vo.setTag("");

        }catch (AuthenticationException e) {
            error.setCode(ResponseCodes.INVALID_TOKEN);
            vo.setError(error);
        }catch(InvalidRequestException e){

            if (e.getValidation() != null) {
                String violation = e.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCodes.INVALID_TOKEN); // if there's no value, set it to the default invalid request parameter
                error.setCode(violation);
            } else {
                error.setCode(ResponseCodes.INVALID_TOKEN);
            }

            vo.setError(error);

        } catch (DisabledAgentPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException |
                 JsonProcessingException |
                 CredentialNotFoundException |
                 GameNotSupportedException e) {
            error.setCode(ResponseCodes.GAME_NOT_ALLOWED);
            vo.setError(error);
        }
        catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            error.setCode(ResponseCodes.GAME_NOT_ALLOWED);
            vo.setError(error);
        }
        finally{
            vo.setUid(loginDto.getUid());
        }

        return vo;
    }

    private void doValidation(LoginDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getArgs());
    }

    private void doVerification(LoginDto dto, GameSession gameSession) throws InvalidRequestException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException, GameNotSupportedException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify vendor gameCode and platform
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGame_id(), GameNotSupportedException::new);
    }
}
