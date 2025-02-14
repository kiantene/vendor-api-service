package com.nextgen.gameaggregator.vendor.dblive.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aglive.constant.Credentials;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public class GameUrlService extends BaseGameUrlService<DbLiveGameUrlVo> {

    protected GameUrlService() {
        super(DbLiveGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        String md5Key = ValidationUtils.validateCredential(credentials.get(Credentials.MD5KEY));
        
        return null;
    }
}
