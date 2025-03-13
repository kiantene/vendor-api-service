package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.EndRoundSettledBet;
import com.nextgen.gameaggregator.entity.ga.EndRoundSettledBetForPatching;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.ProcessEndRoundLog;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.exception.ResponseNotMatchRequestException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.util.RequestLogVo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.web3j.abi.datatypes.Bool;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
@Slf4j
public class TestSupportService {

    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${testing.stub-prefix:load}")
    private String usernamePrefix;

    @Value("${is-test-env:false}")
    private Boolean isTestEnvironment;

    @Value("${spring.profiles.active}")
    private String springEnv;

    public Boolean shouldSkipVendorCall(String username) {
        if (isTestEnvironment()) {
            return username.toLowerCase().startsWith(usernamePrefix.toLowerCase()) || springEnv.contains("preprod");
        }
        return false;
    }

    public Boolean shouldSkipOperatorCall(String username) {
        if (isTestEnvironment()) {
            return username.toLowerCase().startsWith(usernamePrefix.toLowerCase()) || Boolean.TRUE.equals(useStub);
        }
        return false;
    }

    public Boolean isTestEnvironment() {
        return Boolean.TRUE.equals(isTestEnvironment);
    }
}
