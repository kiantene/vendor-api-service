package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.stereotype.Component;

@Component
public class CampaignServiceImpl implements CampaignService {


    @Override
    public Campaign getCampaign(String vendorCampaignCode, Integer vendorId, String currencyCode) {

        return Campaign.builder()
                .id(100)
                .uuid(UuidUtil.newUuidV7StringRaw())
                .build();
    }
}
