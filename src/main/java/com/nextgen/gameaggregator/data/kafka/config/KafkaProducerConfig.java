package com.nextgen.gameaggregator.data.kafka.config;

import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import jakarta.annotation.PostConstruct;
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

    /**
     * Initialises {@link KafkaJsonSchemaSerializerConfig} on the main thread, whose context
     * classloader is Spring Boot's {@code LaunchedURLClassLoader}.
     *
     * <p><strong>DO NOT DELETE — see ONEAPI-834.</strong> This looks like dead code and is not.
     * The class's static initialiser resolves the {@code context.name.strategy} default
     * ({@code io.confluent.kafka.serializers.context.NullContextNameStrategy}) through the
     * <em>thread context classloader</em>. In a fat jar that class lives in {@code BOOT-INF/lib},
     * which the system classloader cannot see. If the class is first initialised on a thread
     * carrying the system classloader — any thread created by a {@code ForkJoinPool.commonPool}
     * worker — the initialiser throws, the JVM marks the class permanently erroneous, and
     * <em>every</em> producer construction for <em>every</em> topic fails until the application
     * is restarted.
     *
     * <p>{@link KafkaProducerWarmUp} now covers this and more, by realising the producers
     * themselves on the startup thread. This guard is kept as the unconditional backstop
     * (ONEAPI-859): the warm-up can be switched off with
     * {@code kafka.producer.warm-up.enabled=false}, and that escape hatch must not also switch
     * off the classloader protection. One {@code Class.forName} with no I/O is a cheap price for
     * that. If the toggle is ever removed, this method can go with it.
     *
     * <p>{@code @PostConstruct} on a {@code @Configuration} singleton is load-bearing: it is
     * strictly ordered before {@code @Scheduled} tasks start (those begin only after context
     * refresh completes). Do not move this to an {@code ApplicationRunner} or
     * {@code CommandLineRunner} — those run after refresh, concurrent with an already-started
     * scheduler, and the ordering guarantee is lost.
     */
    @PostConstruct
    void initSerializerConfigClass() throws ClassNotFoundException {
        Class.forName(KafkaJsonSchemaSerializerConfig.class.getName(), true,
                KafkaProducerConfig.class.getClassLoader());
    }

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

    // api_request_log_v1 can target a different Confluent account (GA-14750). The four
    // connection keys below hold the dedicated (new-account) values and can be configured
    // once and left in place; the single `enabled` toggle decides whether they are used:
    //   enabled=false (default) -> the dedicated producer connects to the CURRENT account
    //                              (shared values), ignoring the overrides
    //   enabled=true            -> it connects to the new account (the override keys; each
    //                              still falls back to the shared value if left unset)
    // This keeps switching a one-property flip + restart, instead of adding/removing four env vars.
    @Value(value = "${spring.kafka.api-request-log.enabled:false}")
    private boolean apiRequestLogDedicatedClusterEnabled;
    @Value(value = "${spring.kafka.api-request-log.bootstrap.servers:${spring.kafka.properties.bootstrap.servers}}")
    private String apiRequestLogBootstrapServers;
    @Value(value = "${spring.kafka.api-request-log.sasl.jaas.config:${spring.kafka.properties.sasl.jaas.config}}")
    private String apiRequestLogSaslJaasConfig;
    @Value(value = "${spring.kafka.api-request-log.schema.registry.url:${spring.kafka.properties.schema.registry.url}}")
    private String apiRequestLogSchemaRegistryUrl;
    @Value(value = "${spring.kafka.api-request-log.basic.auth.user.info:${spring.kafka.properties.basic.auth.user.info}}")
    private String apiRequestLogBasicAuthUserInfo;

    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, this.securityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, this.saslMechanism);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, this.saslJaasConfig);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

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
    public ProducerFactory<String, Object> apiRequestLogProducerFactory() {
        // when the dedicated cluster is disabled, connect to the current account (shared values)
        // and ignore the override keys; when enabled, use the dedicated (new-account) values
        boolean dedicated = this.apiRequestLogDedicatedClusterEnabled;
        HashMap<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, dedicated ? this.apiRequestLogBootstrapServers : this.bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, this.securityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, this.saslMechanism);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, dedicated ? this.apiRequestLogSaslJaasConfig : this.saslJaasConfig);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        props.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, dedicated ? this.apiRequestLogSchemaRegistryUrl : this.schemaRegistryUrl);
        props.put(KafkaJsonSchemaSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, this.basicAuthCredentialsSource);
        props.put(KafkaJsonSchemaSerializerConfig.USER_INFO_CONFIG, dedicated ? this.apiRequestLogBasicAuthUserInfo : this.basicAuthUserInfo);

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

    @Bean
    public KafkaTemplate<String, Object> apiRequestLogKafkaTemplate() {
        return new KafkaTemplate<>(apiRequestLogProducerFactory());
    }
}
