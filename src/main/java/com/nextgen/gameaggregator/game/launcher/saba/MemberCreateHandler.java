package com.nextgen.gameaggregator.game.launcher.saba;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.saba.constant.Credentials;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class MemberCreateHandler extends AbstractGameLaunchHandler<MemberCreateRequest, MemberCreateResponse> {

    public MemberCreateHandler(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, MemberCreateResponse.class);
    }

    @Override
    public void onSuccess(GameLaunchContext context, MemberCreateResponse response) {
        // do nothing
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.CREATE_MEMBER;
    }

    @Override
    public MemberCreateRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return MemberCreateRequest.builder()
                .vendorId(accessor.getValue(Credentials.VENDOR_ID))
                .vendorMemberId(context.getVendorPlayerUsername())
                .operatorId(accessor.getValue(Credentials.OPERATOR_ID))
                .username(context.getVendorPlayerUsername())
                .oddsType(accessor.getValue(Credentials.ODDS_TYPE))
                .currency(context.getVendorCurrencyCode())
                .minTransfer(accessor.getValue(Credentials.MIN_TRANSFER))
                .maxTransfer(accessor.getValue(Credentials.MAX_TRANSFER))
                .build();
    }

    @Override
    public boolean isSuccess(MemberCreateResponse response) {
        return response.isSuccess();
    }
}
