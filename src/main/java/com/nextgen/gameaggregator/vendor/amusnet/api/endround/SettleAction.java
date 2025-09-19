package com.nextgen.gameaggregator.vendor.amusnet.api.endround;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.amusnet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.amusnet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.amusnet.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Set;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SettleAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final UnsettledBetCachingService unsettledBetCachingService;

    @Autowired
    public SettleAction(HttpService httpService,
                        GameSessionService gameSessionService,
                        WalletService walletService,
                        VendorService vendorService,
                        VendorLineService vendorLineService,
                        UnsettledBetCachingService unsettledBetCachingService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.unsettledBetCachingService = unsettledBetCachingService;
    }

    @PostMapping(path = EndPoints.DEPOSIT)
    public String settle(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        SettleVo vo = new SettleVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession;
        BigDecimal balance;

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            SettleDto settleDto = xmlMapper.readValue(body, SettleDto.class);

            // Validate request parameters from vendor (Non-database calls)
            this.doValidation(settleDto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleDto.getPlayerId(), settleDto.getVendorGameId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            ResultType resultType = getResultType(settleDto);
            balance = walletService.processBetResult
                    (httpRequestLog.getId(), gameSession, settleDto, resultType, vendorService, httpRequestLog);

            // Set response data
            vo.setBalance(balance.toBigInteger());
            vo.setCasinoTransferId(httpRequestLog.getId());
            vo.setResponseCodes(ResponseCodes.OK);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.DUPLICATE);

        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.TIME_OUT);

        } catch (Exception e) { // any other exception encountered
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);

        } finally {
            vendorService.buildResponseVo(vo);
            httpService.end(httpRequestLog, vo);
        }

        return vo.getResponseXMLFormat();
    }

    private void doValidation(SettleDto settleDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(settleDto);
    }

    private void doVerification(SettleDto settleDto, GameSession gameSession) throws
            AuthenticationException,
            InvalidRequestException,
            CredentialNotFoundException,
            CurrencyNotSupportedException,
            InvalidCredentialsException,
            BetNotFoundException {

        //1. Currency and Username validation
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), settleDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), settleDto.getPlayerId(), AuthenticationException::new);

        //2. Check for excluded jackpot games
        Set<String> excludedGameIds = Set.of("996", "998", "999", "8888");
        boolean isJackpotGame = excludedGameIds.contains(settleDto.getVendorGameId());

        //3. If not jackpot, verify unsettled bet logic
        if (!isJackpotGame) {
            UnsettledBet unsettledBet = unsettledBetCachingService.getTop1UnsettledBetWithRoundId(settleDto.getGameNumber());

            if (unsettledBet == null) {
                throw new BetNotFoundException();
            }

            if (unsettledBet.getVendorBetId().equals(settleDto.getVendorBetId())) {
                throw new InvalidRequestException();
            }
        }

        //4. Load vendor credentials
        String userName = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSWORD);
        String portalCodeEQ = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE_EQ);
        String portalCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE);
        String categoryCodeList = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CATEGORY_CODE_EQ);

        //5. Validate credentials
        ValidationUtils.isEquals(userName, settleDto.getUserName(), InvalidCredentialsException::new);
        ValidationUtils.isEquals(password, settleDto.getPassword());

        // 6. Validate game code for EQ games (if not jackpot)
        if (!isJackpotGame) {
            String verifiedPortalCode = vendorService.checkGameCodeIsOpenEQGame(
                    categoryCodeList,
                    settleDto.getVendorGameId(),
                    portalCodeEQ,
                    portalCode
            );

            ValidationUtils.isEquals(verifiedPortalCode, settleDto.getPortalCode());
        }
    }

    private ResultType getResultType(SettleDto settleDto) {
        ResultType resultType = ResultType.BET_LOSE;
        if (settleDto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }

}
