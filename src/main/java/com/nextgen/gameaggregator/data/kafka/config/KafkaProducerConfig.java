package com.nextgen.gameaggregator.data.kafka.config;

import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;

@Configuration
public class KafkaProducerConfig {

    @Value(value = "${spring.kafka.properties.bootstrap.servers}")
    private String bootstrapServers;
    @Value(value = "${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;
    @Value(value = "${spring.kafka.properties.sasl.jaas.config}")
    private String saslJaasConfig;
    @Value(value = "${spring.kafka.properties.security.protocol}")
    private String securityProtocol;
    @Value(value = "${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;
    @Value(value = "${spring.kafka.properties.basic.auth.credentials.source}")
    private String basicAuthCredentialsSource;
    @Value(value = "${spring.kafka.properties.basic.auth.user.info}")
    private String basicAuthUserInfo;

    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, this.securityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, this.saslMechanism);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, this.saslJaasConfig);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ProducerFactory<String, Object> jsonSchemaProducerFactory() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, this.securityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, this.saslMechanism);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, this.saslJaasConfig);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class);
//        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 10000);
//        props.put(ProducerConfig.LINGER_MS_CONFIG, 3000);
//        props.put(ProducerConfig.RETRIES_CONFIG, 15);
//        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 5000);
//        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        props.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistryUrl);
        props.put(KafkaJsonSchemaSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, this.basicAuthCredentialsSource);
        props.put(KafkaJsonSchemaSerializerConfig.USER_INFO_CONFIG, this.basicAuthUserInfo);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Object> jsonSchemaKafkaTemplate() {
        return new KafkaTemplate<>(jsonSchemaProducerFactory());
    }
}
