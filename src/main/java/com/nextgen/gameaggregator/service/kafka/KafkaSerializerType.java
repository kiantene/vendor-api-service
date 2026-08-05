package com.nextgen.gameaggregator.service.kafka;

/**
 * Identifies which KafkaTemplate produced a message, so a persisted fallback
 * record can be replayed through the same serializer it originally used.
 *
 * <ul>
 *     <li>{@link #STRING} — {@code stringKafkaTemplate} (value already a JSON String).</li>
 *     <li>{@link #JSON_SCHEMA} — {@code jsonSchemaKafkaTemplate} (value is an object serialized
 *         by the Confluent JSON-Schema serializer; replayed by re-deserializing to its class).</li>
 * </ul>
 */
public enum KafkaSerializerType {
    STRING,
    JSON_SCHEMA
}
