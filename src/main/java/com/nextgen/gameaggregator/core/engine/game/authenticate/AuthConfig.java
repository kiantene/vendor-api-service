package com.nextgen.gameaggregator.core.engine.game.authenticate;

import lombok.Getter;

public class AuthConfig {
    private boolean refreshToken;
    @Getter
    private String replaceTokenWith;

    public AuthConfig() {
        this.refreshToken = false;
    }

    public AuthConfig refreshToken(boolean flag) {
        this.refreshToken = flag;
        return this;
    }

    public boolean shouldRefreshToken() {
        return this.refreshToken;
    }

    public AuthConfig replaceTokenWith(String newToken) {
        this.replaceTokenWith = newToken;
        return this;
    }
}
