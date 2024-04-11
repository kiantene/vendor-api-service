package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public interface SportBetDetail<T> {
    MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo,
                                                  VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException;

    T call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo,
           VendorLanguageCode vendorLanguageCode) throws InvalidVendorResponseException, InvalidVendorLineException;
}
