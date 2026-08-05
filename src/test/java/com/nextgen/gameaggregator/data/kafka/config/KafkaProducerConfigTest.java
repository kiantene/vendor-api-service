package com.nextgen.gameaggregator.data.kafka.config;

import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.CurrencyConversionService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.S3BetService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaProducerConfigTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void apiRequestLogFactoryFallsBackToSharedConnectionValuesWhenNoOverrides() {
        context = buildContext(Map.of());

        Map<String, Object> shared = producerFactory("jsonSchemaProducerFactory").getConfigurationProperties();
        Map<String, Object> dedicated = producerFactory("apiRequestLogProducerFactory").getConfigurationProperties();

        assertThat(dedicated).isEqualTo(shared);
    }

    @Test
    void apiRequestLogFactoryIgnoresOverridesWhenDisabled() {
        // overrides configured but the toggle is off (default) -> dedicated producer stays on
        // the current account, so it must equal the shared factory config
        context = buildContext(Map.of(
                "spring.kafka.api-request-log.bootstrap.servers", "nea-bootstrap:9092",
                "spring.kafka.api-request-log.sasl.jaas.config", "nea-jaas",
                "spring.kafka.api-request-log.schema.registry.url", "http://nea-registry",
                "spring.kafka.api-request-log.basic.auth.user.info", "nea-key:nea-secret"
        ));

        Map<String, Object> shared = producerFactory("jsonSchemaProducerFactory").getConfigurationProperties();
        Map<String, Object> dedicated = producerFactory("apiRequestLogProducerFactory").getConfigurationProperties();

        assertThat(dedicated).isEqualTo(shared);
    }

    @Test
    void apiRequestLogFactoryUsesOverridesForTheFourConnectionKeysOnlyWhenEnabled() {
        context = buildContext(Map.of(
                "spring.kafka.api-request-log.enabled", "true",
                "spring.kafka.api-request-log.bootstrap.servers", "nea-bootstrap:9092",
                "spring.kafka.api-request-log.sasl.jaas.config", "nea-jaas",
                "spring.kafka.api-request-log.schema.registry.url", "http://nea-registry",
                "spring.kafka.api-request-log.basic.auth.user.info", "nea-key:nea-secret"
        ));

        Map<String, Object> shared = producerFactory("jsonSchemaProducerFactory").getConfigurationProperties();
        Map<String, Object> dedicated = producerFactory("apiRequestLogProducerFactory").getConfigurationProperties();

        assertThat(dedicated.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("nea-bootstrap:9092");
        assertThat(dedicated.get(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo("nea-jaas");
        assertThat(dedicated.get(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG)).isEqualTo("http://nea-registry");
        assertThat(dedicated.get(KafkaJsonSchemaSerializerConfig.USER_INFO_CONFIG)).isEqualTo("nea-key:nea-secret");

        // shared factory keeps the shared values
        assertThat(shared.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("shared-bootstrap:9092");
        assertThat(shared.get(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo("shared-jaas");
        assertThat(shared.get(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG)).isEqualTo("http://shared-registry");
        assertThat(shared.get(KafkaJsonSchemaSerializerConfig.USER_INFO_CONFIG)).isEqualTo("shared-user:shared-secret");

        // non-connection settings stay identical to the shared factory
        assertThat(dedicated.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(shared.get(SaslConfigs.SASL_MECHANISM));
        assertThat(dedicated.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isEqualTo(shared.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertThat(dedicated.get(KafkaJsonSchemaSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE)).isEqualTo(shared.get(KafkaJsonSchemaSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE));
        assertThat(dedicated.get(ProducerConfig.MAX_BLOCK_MS_CONFIG)).isEqualTo(shared.get(ProducerConfig.MAX_BLOCK_MS_CONFIG));
        assertThat(dedicated.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(shared.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertThat(dedicated.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(shared.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }

    @Test
    void kafkaServiceWiresTheIntendedTemplateBeans() {
        context = buildContext(Map.of(
                "spring.kafka.api-request-log.bootstrap.servers", "nea-bootstrap:9092"
        ));

        KafkaService kafkaService = context.getBean(KafkaService.class);

        assertThat(ReflectionTestUtils.getField(kafkaService, "stringKafkaTemplate"))
                .isSameAs(context.getBean("stringKafkaTemplate"));
        assertThat(ReflectionTestUtils.getField(kafkaService, "jsonSchemaKafkaTemplate"))
                .isSameAs(context.getBean("jsonSchemaKafkaTemplate"));
        assertThat(ReflectionTestUtils.getField(kafkaService, "apiRequestLogKafkaTemplate"))
                .isSameAs(context.getBean("apiRequestLogKafkaTemplate"));
    }

    private ProducerFactory<?, ?> producerFactory(String beanName) {
        return context.getBean(beanName, ProducerFactory.class);
    }

    private AnnotationConfigApplicationContext buildContext(Map<String, Object> overrides) {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.kafka.properties.bootstrap.servers", "shared-bootstrap:9092");
        props.put("spring.kafka.properties.sasl.mechanism", "PLAIN");
        props.put("spring.kafka.properties.sasl.jaas.config", "shared-jaas");
        props.put("spring.kafka.properties.security.protocol", "SASL_SSL");
        props.put("spring.kafka.properties.schema.registry.url", "http://shared-registry");
        props.put("spring.kafka.properties.basic.auth.credentials.source", "USER_INFO");
        props.put("spring.kafka.properties.basic.auth.user.info", "shared-user:shared-secret");
        props.putAll(overrides);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-props", props));
        // mocks registered as plain singletons so Spring skips their own @Autowired processing
        ctx.getBeanFactory().registerSingleton("currencyConversionService", mock(CurrencyConversionService.class));
        ctx.getBeanFactory().registerSingleton("warehouseBetHistoryService", mock(WarehouseBetHistoryService.class));
        ctx.getBeanFactory().registerSingleton("agentPlayerService", mock(AgentPlayerService.class));
        ctx.getBeanFactory().registerSingleton("vendorPlayerService", mock(VendorPlayerService.class));
        ctx.getBeanFactory().registerSingleton("s3BetService", mock(S3BetService.class));
        ctx.register(PropertySourcesPlaceholderConfigurer.class, KafkaProducerConfig.class, KafkaService.class);
        ctx.refresh();
        return ctx;
    }
}
