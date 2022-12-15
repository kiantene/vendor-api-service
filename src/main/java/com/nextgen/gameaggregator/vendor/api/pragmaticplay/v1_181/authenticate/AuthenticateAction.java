package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.grpc.v1.operator.walletbalance.WalletBalanceGrpcVo;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.action.AbstractAction;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.authenticate.AuthenticationDto;
import com.nextgen.gameaggregator.vendor.exception.InvalidRequestException;
import com.nextgen.gameaggregator.vendor.grpc.v1.subcriber.OperatorWalletBalanceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.VENDOR_CODE;

@RestController
@RequestMapping(path = "api/v2/pragmaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class AuthenticateAction {
    @Autowired
    HttpServletRequest request;

    @PostMapping(path = "authenticate")
    public AuthenticateVo authenticate(AuthenticateDto dto) throws IOException {
        // temporary solution to convert to DTO
        String body = request.getReader().lines().collect(Collectors.joining());

        dto = this.queryStringToDto(body, AuthenticateDto.class);
        log.info(dto.toString());

        AuthenticateVo response = new AuthenticateVo();
        response.setError(0);
        response.setDescription("Success");

        try {
            this.validateRequest(dto);
        } catch (InvalidRequestException invalidRequestException) {
            response.setError(7);
            response.setDescription("Bad parameters in the request, please check post parameters.");
        }

        response.setUserId("421");
        response.setCurrency("USD");
        response.setCash(new BigDecimal("99999.99"));
        response.setBonus(new BigDecimal("99.99"));

        return response;
    }

    public void validateRequest(AuthenticateDto dto) throws InvalidRequestException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<AuthenticateDto>> violations = validator.validate(dto);

        log.info(violations.isEmpty() ? "true" : "false");

        if (!violations.isEmpty()) { // request param missing
            throw new InvalidRequestException();
        }
    }

    public <T> T queryStringToDto(String queryString, Class<T> clazz) {

        log.info(queryString);

        HashMap<String, Object> queryParameterMap = new HashMap<String, Object>();
        String[] fields = queryString.split("&");

        for (int i = 0; i < fields.length; ++i) {
            String[] kv = fields[i].split("=");
            if (2 == kv.length) {
                queryParameterMap.put(kv[0], kv[1]);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        T t = mapper.convertValue(queryParameterMap, clazz);

        return t;
    }
}
