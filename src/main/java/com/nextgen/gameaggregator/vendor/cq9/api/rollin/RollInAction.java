package com.nextgen.gameaggregator.vendor.cq9.api.rollin;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletTransactionService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.*;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollInAction {

    private final HttpService httpService;
    private final WalletRequestService walletRequestService;
    private final VendorLineService vendorLineService;
    private final OperatorWalletService operatorWalletService;
    private final WalletTransactionService walletTransactionService;

    public RollInAction(HttpService httpService,
                        VendorLineService vendorLineService,
                        WalletRequestService walletRequestService,
                        OperatorWalletService operatorWalletService,
                        WalletTransactionService walletTransactionService) {

        this.httpService = httpService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
        this.vendorLineService = vendorLineService;
        this.walletTransactionService = walletTransactionService;
    }

    private void dataMapper(WalletRequest walletRequest, RollInDto dto) throws InvalidPlayerException, BetNotAllowedException, InternalServerException {

        // TODO: check updateByVendorUsername should verify agent credential status for credit?
        walletRequestService.updateByVendorUsername(walletRequest, dto.getVendorPlayerUsername());
        walletRequestService.updateByVendorGameCode(walletRequest, dto.getGamecode(), false);
        walletRequestService.updateByCurrencyId(walletRequest, walletRequest.getCurrencyId());

        walletRequest.setExternalTransactionId(dto.getMtcode());
        walletRequest.setRoundId(dto.getRoundid());
        walletRequest.setTimestamp(dto.getTimestamp());
        walletRequest.setVendorBetId(dto.getRoundid());
        walletRequest.setTransferAmount(dto.getAmount());
        walletRequest.setVendorBetId(dto.getRoundid());
        walletRequest.setBetAmount(dto.getBet());
        walletRequest.setWinAmount(dto.getWin());
        walletRequest.setEffectiveTurnover(dto.getEffectiveTurnover());
        walletRequest.setJackpotAmount(BigDecimal.ZERO);
        walletRequest.setResultType(ResultType.BET_WIN.code);
        walletRequest.setVendorBetTime(dto.getTimestamp());
        walletRequest.setVendorSettleTime(dto.getTimestamp());

        String gameType = dto.getGametype();

        // for Live and Table games,
        if (gameType.equals(GameType.LIVE) || gameType.equals(GameType.TABLE)) {
            BigDecimal rake = dto.getRake();
            BigDecimal winLoss = dto.getWin().subtract(rake);
            BigDecimal winAmount = winLoss.add(dto.getBet());

            walletRequest.setWinAmount(winAmount);
            walletRequest.setWinLoss(winLoss);
        }

        WalletTransaction walletTransaction = walletTransactionService.getByRoundIdAndVendorPlayerUsername(walletRequest.getRoundId(), walletRequest.getVendorPlayerUsername());

        if (walletTransaction == null) {
            walletRequest.setToken(walletRequest.getTraceId());
        } else {
            walletRequest.setToken(walletTransaction.getToken());
        }

    }

    @PostMapping(path = EndPoints.ROLLIN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    private ResponseVo<CommonVo> rollIn(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        String wToken = request.getHeader("wtoken");

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        String errorMessage = "";

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            RollInDto rollInDto = HttpService.convertQueryStringToDtoUrlDecode(body, RollInDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(rollInDto, wToken);

            // add request idempotent check
            httpService.isDuplicateRequest(rollInDto);

            this.dataMapper(walletRequest, rollInDto);

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(walletRequest, wToken);

            walletRequest = operatorWalletService.betCredit(walletRequest);

            // Construct VO data
            CommonVo commonVo = new CommonVo();
            commonVo.setBalance(walletRequest.getBalanceAfter());
            commonVo.setCurrency(walletRequest.getCurrencyCode());
            responseVo.setData(commonVo);

        } catch (DuplicateRequestException duplicateRequestException) {
            statusVo.setCode(ResponseCodes.SUCCESS); // vendor requested to return success
            errorMessage = duplicateRequestException.getMessage();

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            errorMessage = invalidRequestException.getMessage();

        } catch (AuthenticationException |
                 InvalidPlayerException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            errorMessage = authenticationException.getMessage();

        } catch (DateTimeParseException dateTimeParseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);
            errorMessage = dateTimeParseException.getMessage();

        } catch (InternalServerException | InvalidOperatorResponseException internalServerException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            errorMessage = internalServerException.getMessage();

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            errorMessage = exception.getMessage();

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            if (StringUtils.hasText(errorMessage)) {
                walletRequest.setErrorMessage(errorMessage);
            }
            walletRequestService.end(walletRequest, httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RollInDto dto, String wToken) throws InvalidRequestException, DateTimeParseException {
        if (wToken == null) throw new InvalidRequestException("wToken is missing");

        // General validation
        ValidationUtils.validateRequest(dto);

        DateTimeFormatter.ISO_DATE_TIME.parse(dto.getEventTime());
        DateTimeFormatter.ISO_DATE_TIME.parse(dto.getCreateTime());

        // 5. Validate win amount
        if (dto.getGametype().equals(GameType.FISH) || dto.getGametype().equals(GameType.ARCADE)) {
            if (dto.getWin().compareTo(BigDecimal.ZERO) < 0) throw new InvalidRequestException();
        }
    }

    private void doVerification(WalletRequest walletRequest, String wToken) throws AuthenticationException, InternalServerException {

        try {
            // 1. Retrieve vendor line credentials and secretKey for verify API Token
            String walletToken = vendorLineService.getCredentialValueByName(walletRequest.getVendorLineId(), Credentials.WALLET_TOKEN);

            // 2. Validate request Wallet Token
            ValidationUtils.isEquals(walletToken, wToken, AuthenticationException::new);

        } catch (CredentialNotFoundException exception) {

            throw new InternalServerException(exception.getClass() + " : " + exception.getMessage());
        }
    }
}

