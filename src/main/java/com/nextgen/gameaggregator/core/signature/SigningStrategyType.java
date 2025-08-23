package com.nextgen.gameaggregator.core.signature;

/**
 * @deprecated use {@link com.nextgen.core.security.signature.SigningStrategyType} instead
 */
@Deprecated(forRemoval = true)
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
    @Deprecated
    HMAC_SHA256 {
        @Override
        public SignatureStrategy getStrategy() {
            return new HmacSha256SignatureStrategy(HmacSha256SignatureStrategy.EncodingType.HEX);
        }
    },
    HMAC_SHA256_BASE64 {
        @Override
        public SignatureStrategy getStrategy() {
            return new HmacSha256SignatureStrategy(HmacSha256SignatureStrategy.EncodingType.BASE64);
        }
    },
    HMAC_SHA256_HEX {
        @Override
        public SignatureStrategy getStrategy() {
            return new HmacSha256SignatureStrategy(HmacSha256SignatureStrategy.EncodingType.HEX);
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
