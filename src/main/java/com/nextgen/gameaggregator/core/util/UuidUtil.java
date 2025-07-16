package com.nextgen.gameaggregator.core.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class UuidUtil {

    private UuidUtil() {
        // Utility class — prevent instantiation
    }

    /**
     * Generate a UUIDv7 (time-ordered).
     */
    public static UUID newUuidV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /**
     * Generate UUIDv7 and return it as a standard UUID string (with hyphens).
     * Example: 01890f3b-9c3c-7cc2-92b5-739b6d086bdc
     */
    public static String newUuidV7String() {
        return newUuidV7().toString();
    }

    /**
     * Generate UUIDv7 and return it as a 32-character string (no hyphens).
     * Example: 01890f3b9c3c7cc292b5739b6d086bdc
     */
    public static String newUuidV7StringRaw() {
        return newUuidV7().toString().replace("-", "");
    }

    /**
     * Generate UUIDv7 and return it as a 16-byte array.
     */
    public static byte[] newUuidV7Bytes() {
        return uuidToBytes(newUuidV7());
    }

    /**
     * Convert UUID to a 16-byte array (big-endian).
     */
    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * Convert a 16-byte array back to UUID.
     */
    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("UUID byte array must be 16 bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long high = buffer.getLong();
        long low = buffer.getLong();
        return new UUID(high, low);
    }
}
