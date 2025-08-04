package com.nextgen.gameaggregator.core.signature;

public enum SigningStrategyType {
    MD5 {
        @Override
        public SignatureStrategy getInstance() {
            return new Md5SignatureStrategy();
        }
    };

    public abstract SignatureStrategy getInstance();
}
