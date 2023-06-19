package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class QueryAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private BetResultLogService betResultLogService;

    @PostMapping(path = EndPoints.QUERY)
    public QueryVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        QueryVo responseVo = new QueryVo();
        FundTransferResponseVo fundTransferResponseVo = new FundTransferResponseVo();
        StatusVo statusVo = new StatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            QueryDto queryDto = HttpService.convertJsonToDto(body, QueryDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(queryDto);

            //Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(queryDto.getQueryRequestDto().getAccountId(), queryDto.getBaseGame().getKeyName());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(queryDto, gameSession);

            //Check bet record available from settle and unsettle table
            this.checkBetAvailable(gameSession, queryDto.getQueryRequestDto());

            //return success respond
            statusVo.setSuccess(true);

        } catch (
                AuthenticationException |
                InvalidRequestException |
                NoAvailableLineException |
                JsonProcessingException |
                CredentialNotFoundException |
                BetNotFoundException generalException
        ) {
            statusVo.setSuccess(false);
        } catch (Exception exception) {
            statusVo.setSuccess(false);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(QueryDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getBaseGame());
        ValidationUtils.validateRequest(dto.getSubAuth());
        ValidationUtils.validateRequest(dto.getQueryRequestDto());
        ValidationUtils.isEquals("queryrequest", dto.getType(), InvalidRequestException::new);
    }

    private void doVerification(QueryDto dto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException {

        //Verify received passkey is the same from credential
        String passkey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSKEY);
        ValidationUtils.isEquals(passkey, dto.getSubAuth().getPasskey(), NoAvailableLineException::new);

        //Verify received brand id is the same from credential
        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        ValidationUtils.isEquals(brandId, dto.getSubAuth().getBrandid(), NoAvailableLineException::new);

    }

    private void checkBetAvailable(GameSession gameSession, QueryRequestDto queryRequestDto) throws BetNotFoundException {

        try {
            //check settle bet available
            SettledBet settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), queryRequestDto.getInitialDebitTransferId());
        } catch (BetNotFoundException betNotFoundException) {
            try {
                //check unsettle bet available
                UnsettledBet unsettledBet = unsettledBetService.getByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), queryRequestDto.getTransferId());
            } catch (BetNotFoundException unsettledBetNotFoundException) {
                //check unsettle bet result available
                RawBetResultLog rawBetResultLog = betResultLogService.checkExists(queryRequestDto.getTransferId(), queryRequestDto.getGameInstanceId(), gameSession.getVendorGameId().toString(), gameSession.getVendorPlayerId().toString());
                if (rawBetResultLog == null) {
                    throw betNotFoundException;
                }
            }
        }
    }
}
