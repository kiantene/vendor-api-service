package com.nextgen.gameaggregator.vendor.aglive.api.betdetail;

import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public class BetDetailService implements BetDetailUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
                                                         IBetDetailUrlInfo iBetDetailUrlInfo,
                                                         VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        return new LinkedMultiValueMap<>();
    }

    @Override
    public com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo call(MultiValueMap<String, String> formData,
                                                                                       Map<String, String> credentials,
                                                                                       IBetDetailUrlInfo iBetDetailUrlInfo,
                                                                                       VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        return new BetDetailVo();
    }
}