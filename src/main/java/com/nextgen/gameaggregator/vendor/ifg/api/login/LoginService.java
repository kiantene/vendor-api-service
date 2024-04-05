package com.nextgen.gameaggregator.vendor.ifg.api.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import com.nextgen.gameaggregator.vendor.ifg.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    @Autowired
    private VendorService vendorService;

    public CommonVo login(HttpRequestLog httpRequestLog, String traceId){
        LoginServiceDto loginDto = new LoginServiceDto();
        LoginServiceVo vo = new LoginServiceVo();
        BalanceVo balanceVo = new BalanceVo();
        EnterVo enterVo = new EnterVo();
        UserVo userVo = new UserVo();
        ErrorVo errorVo = new ErrorVo();
        XmlMapper xmlMapper = new XmlMapper();

        try{
            loginDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), LoginServiceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(loginDto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(loginDto.getEnter().getKey());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(loginDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // replace this token value by using guid value(vendor does not return our game session)
            gameSessionService.regenerateGameSessionToken(gameSession, loginDto.getEnter().getGuid());

            // set userVo
            userVo.setType("real");
            userVo.setWlid(gameSession.getVendorPlayerUsername());

            // set balanceVo
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setType("real");
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            balanceVo.setValue(String.valueOf(balance.intValue()));

            // set enterVo
            enterVo.setId(loginDto.getEnter().getId());
            enterVo.setResult(ResponseCodes.RESULT_SUCCESS);
            enterVo.setBalance(balanceVo);
            enterVo.setUser(userVo);

            // set vo
            vo.setEnter(enterVo);

        } catch (AuthenticationException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.INVALID_KEY);
            errorVo.setMsg(ResponseCodes.I_K);

            // set enterVo
            enterVo.setId(loginDto.getEnter().getId());
            enterVo.setResult(ResponseCodes.RESULT_FAIL);
            enterVo.setError(errorVo);

            // set vo
            vo.setEnter(enterVo);

            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e){

            // set errorVo
            if (e.getValidation() != null) {
                String violation = e.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCodes.INVALID_KEY); // if there's no value, set it to the default invalid request parameter

                errorVo.setCode(violation);

                if(violation.equals(ResponseCodes.INVALID_KEY)){
                    errorVo.setMsg(ResponseCodes.I_K);
                }else{
                    errorVo.setMsg(ResponseCodes.G_N_A);
                }

                httpService.logError(httpRequestLog, e);
            } else {
                // set errorVo
                errorVo.setCode(ResponseCodes.INVALID_KEY);
                errorVo.setMsg(ResponseCodes.I_K);
            }

            // set enterVo
            enterVo.setId(loginDto.getEnter().getId());
            enterVo.setResult(ResponseCodes.RESULT_FAIL);
            enterVo.setError(errorVo);

            // set vo
            vo.setEnter(enterVo);

        } catch (GameNotSupportedException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException |
                 JsonProcessingException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.GAME_NOT_ALLOWED);
            errorVo.setMsg(ResponseCodes.G_N_A);

            // set enterVo
            enterVo.setId(loginDto.getEnter().getId());
            enterVo.setResult(ResponseCodes.RESULT_FAIL);
            enterVo.setError(errorVo);

            // set vo
            vo.setEnter(enterVo);

            httpService.logError(httpRequestLog, e);
        } catch(Exception e) {

            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set enterVo
            enterVo.setId(loginDto.getEnter().getId());
            enterVo.setResult(ResponseCodes.RESULT_ERROR);
            enterVo.setError(errorVo);

            // set vo
            vo.setEnter(enterVo);

            httpService.logError(httpRequestLog, e);
        } finally{
            // set vo
            vo.setSession(loginDto.getSession());
            vo.setTime(loginDto.getTime());
        }

        return vo;
    }

    private void doValidation(LoginServiceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getEnter());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getEnter().getGame());
    }

    private void doVerification(LoginServiceDto dto, GameSession gameSession) throws InvalidRequestException, DisabledVendorLineException, DisabledGameException, AuthenticationException, GameNotSupportedException, DisabledAgentPlayerException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getEnter().getGame().getName(), GameNotSupportedException::new);

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());
    }
}
