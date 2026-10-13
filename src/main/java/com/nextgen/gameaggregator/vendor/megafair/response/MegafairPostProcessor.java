package com.nextgen.gameaggregator.vendor.megafair.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.megafair.api.authenticate.AuthenticateRequest;
import com.nextgen.gameaggregator.vendor.megafair.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.megafair.config.MegafairConfig;
import com.nextgen.gameaggregator.vendor.megafair.constant.Formats;
import com.nextgen.gameaggregator.vendor.megafair.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.megafair.constant.Results;
import com.nextgen.gameaggregator.vendor.megafair.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.nextgen.gameaggregator.vendor.megafair.constant.MegafairFields.*;

@Component
@Slf4j
public class MegafairPostProcessor implements VendorResponsePostProcessor {

    private final VendorUtil vendorUtil;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter VENDOR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT).withLocale(Locale.ENGLISH);

    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            AuthenticateRequest.class,
            CommonRequest.class,
            RollbackRequest.class
    );

    public MegafairPostProcessor(VendorUtil vendorUtil, ObjectMapper objectMapper) {
        this.vendorUtil = vendorUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getVendorClassName() {
        return MegafairConfig.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse postProcessErrorResponse(VendorErrorResponse errorResponse,
                                                        VendorExceptionContext errorContext) {
        log.debug("Post-processing error response for vendor: {}", getVendorClassName());

        if (!(errorResponse.getBody() instanceof ErrorResponse body)) {
            log.warn("Response body is not an instance of ErrorResponse, skipping enrichment");
            return errorResponse;
        }

        errorContext.getAnyPresentClass(REQUEST_CLASSES).ifPresent(request -> {
            if (request instanceof AuthenticateRequest authReq) {
                enrichErrorResponse(body, authReq);
            } else if (request instanceof CommonRequest commonReq) {
                enrichErrorResponse(body, commonReq);
            } else if (request instanceof RollbackRequest rollbackReq) {
                enrichErrorResponse(body, rollbackReq);
            }
        });

        return vendorUtil.toXmlVendorResponse(errorResponse.getStatusCode(), body);
    }

    @Override
    public VendorErrorResponse postProcessInvalidRequest(InvalidRequestContext ctx) {
        ErrorResponse.RequestData requestData = ErrorResponse.RequestData.builder()
                .userId(ctx.getQueryParam(USER_ID).orElse(null))
                .bet(ctx.getQueryParam(BET).orElse(null))
                .win(ctx.getQueryParam(WIN).orElse(null))
                .roundId(ctx.getQueryParam(ROUND_ID).orElse(null))
                .gameId(ctx.getQueryParam(GAME_ID).orElse(null))
                .gameSessionId(ctx.getQueryParam(GAME_SESSION_ID).orElse(null))
                .isRoundFinished(ctx.getQueryParam(IS_ROUND_FINISHED).orElse(null))
                .kind(ctx.getQueryParam(KIND).orElse(null))
                .token(ctx.getQueryParam(TOKEN).orElse(null))
                .hash(ctx.getQueryParam(HASH).orElse(null))
                .casinoTransactionId(ctx.getQueryParam(CASINO_TRANSACTION_ID).orElse(null))
                .build();

        Map<String, Object> responseBody = ctx.getResponseBody();

        ErrorResponse.ResponseData responseData = objectMapper.convertValue(
                responseBody.get(RESPONSE),
                ErrorResponse.ResponseData.class
        );

        if (responseData == null) {
            responseData = ErrorResponse.ResponseData.builder()
                    .result(Results.ERROR)
                    .code(String.valueOf(ResponseCodes.INTERNAL_ERROR_ES.getCode()))
                    .build();
        }

        String time = Objects.toString(
                responseBody.get(TIME),
                VENDOR_DATE_FORMATTER.format(ZonedDateTime.now())
        );

        ErrorResponse response = ErrorResponse.builder()
                .request(requestData)
                .time(time)
                .response(responseData)
                .build();

        return vendorUtil.toXmlVendorResponse(HttpStatus.OK, response);
    }

    private void enrichErrorResponse(ErrorResponse response, AuthenticateRequest authReq) {
        if (response.getRequest() == null) {
            response.setRequest(ErrorResponse.RequestData.builder().build());
        }
        response.getRequest().setToken(authReq.getToken());
        response.getRequest().setGameSessionId(authReq.getGameSessionId());
        response.getRequest().setHash(authReq.getHash());
    }

    private void enrichErrorResponse(ErrorResponse response, CommonRequest commonReq) {
        if (response.getRequest() == null) {
            response.setRequest(ErrorResponse.RequestData.builder().build());
        }

        ErrorResponse.RequestData requestData = response.getRequest();

        requestData.setUserId(commonReq.getUserId());
        requestData.setBet(commonReq.getBet());
        requestData.setWin(commonReq.getWin());
        requestData.setRoundId(commonReq.getRoundId());
        requestData.setGameId(commonReq.getGameId());
        requestData.setGameSessionId(commonReq.getGameSessionId());
        requestData.setKind(commonReq.getKind());
        requestData.setHash(commonReq.getHash());
        requestData.setIsRoundFinished(commonReq.getIsRoundFinished());

    }

    private void enrichErrorResponse(ErrorResponse response, RollbackRequest rollbackReq) {
        if (response.getRequest() == null) {
            response.setRequest(ErrorResponse.RequestData.builder().build());
        }

        ErrorResponse.RequestData requestData = response.getRequest();

        requestData.setUserId(rollbackReq.getUserId());
        requestData.setCasinoTransactionId(rollbackReq.getCasinoTransactionId());
        requestData.setGameSessionId(rollbackReq.getGameSessionId());
        requestData.setHash(rollbackReq.getHash());
    }
}
