package com.nextgen.gameaggregator.game.launcher.saba;

import com.nextgen.gameaggregator.core.common.WebClientApiCaller;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;

public class GameLauncher implements GameLaunchHandler {
    private final WebClientApiCaller webClientApiCaller;

    public GameLauncher() {
        this.webClientApiCaller = new WebClientApiCaller(EndPoints.GAME_URL, getContentType());
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public Object onPrepareRequestBody(GameLaunchContext context) {
        return null;
    }

    @Override
    public void onSuccess() {

    }

    public void getUrl() {
//        webClientApiCaller.post()
    }
}
