package com.nextgen.gameaggregator.core.signature;

public enum SigningStrategyType {
    MD5 {
        @Override
        public SignatureStrategy getStrategy() {
            return new Md5SignatureStrategy();
        }
    },
    HMAC_SHA256 {
        @Override
        public SignatureStrategy getStrategy() {
            return new HmacSha256SignatureStrategy();
        }
    },
    NO_OP {
        @Override
        public SignatureStrategy getStrategy() {
            return new NoOpSignatureStrategy();
        }
    };

    public abstract SignatureStrategy getStrategy();
}
