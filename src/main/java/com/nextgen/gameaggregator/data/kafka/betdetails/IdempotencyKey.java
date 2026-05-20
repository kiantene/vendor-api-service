package com.nextgen.gameaggregator.data.kafka.betdetails;

// Stage-1 producers (C.1 OpenSearch tail + C.3 in-service emit) MUST compute the
// same key so Stage-2 can dedupe across the parallel-run window. Keep this
// formula in lockstep with the C.1 implementation.
public final class IdempotencyKey {

    private IdempotencyKey() {
    }

    public static String of(String vendor, String vendorBetId, EventKind eventKind) {
        return vendor + ":" + vendorBetId + ":" + eventKind.name();
    }

    // For re-issued/amended result events (e.g. Pinnacle resettles): distinguish each occurrence
    // so the resettle does not collide with the original settle's idempotency key.
    public static String of(String vendor, String vendorBetId, EventKind eventKind, Long version) {
        String base = of(vendor, vendorBetId, eventKind);
        return version == null ? base : base + ":v" + version;
    }
}
