package com.nextgen.gameaggregator.core.signature;

public enum SigningStrategyType {
    MD5 {
        @Override
        public SignatureStrategy getStrategy() {
            return new Md5SignatureStrategy();
        }
    },
    MD5_REVERSE {
        @Override
        public SignatureStrategy getStrategy() {
            return new Md5SignatureStrategy(Md5SignatureStrategy.ConcatenationOrder.SECRET_PAYLOAD);
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
