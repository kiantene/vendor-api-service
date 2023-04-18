package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public interface BetDetailUrl {
    MultiValueMap<String, String> formDataBuilder( Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException;
    BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo) throws InvalidVendorResponseException, InvalidVendorLineException;
}

