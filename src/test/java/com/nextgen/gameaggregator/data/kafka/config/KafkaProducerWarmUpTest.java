package com.nextgen.gameaggregator.data.kafka.config;

import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ProducerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards the warm-up behaviour introduced for ONEAPI-859.
 *
 * <p>The classloader guarantee itself is covered by {@link KafkaSerializerClassInitTest}; this
 * class covers what the warm-up must do around it — realise every factory, and never take the
 * application down when it cannot.
 */
class KafkaProducerWarmUpTest {

    @Test
    void realisesEveryProducerFactory() {
        CountingFactory a = new CountingFactory(false);
        CountingFactory b = new CountingFactory(false);

        new KafkaProducerWarmUp(factories("a", a, "b", b), true).afterSingletonsInstantiated();

        assertThat(a.calls.get()).isEqualTo(1);
        assertThat(b.calls.get()).isEqualTo(1);
    }

    /**
     * The whole point of catching (ONEAPI-859). A warm-up failure is almost always
     * {@code bootstrap.servers} not resolving at boot; aborting startup would take the fleet down
     * on a DNS blip, for a pipeline that is not on the bet or launch path. The serializer classes
     * are initialised before bootstrap addresses are resolved, so a throwing warm-up has still
     * done its job.
     */
    @Test
    void aFailingFactoryDoesNotAbortStartup() {
        CountingFactory failing = new CountingFactory(true);
        CountingFactory healthy = new CountingFactory(false);

        assertThatCode(() ->
                new KafkaProducerWarmUp(factories("failing", failing, "healthy", healthy), true).afterSingletonsInstantiated())
                .doesNotThrowAnyException();

        assertThat(healthy.calls.get())
                .as("a failure must not stop the remaining factories being warmed")
                .isEqualTo(1);
    }

    @Test
    void disabledSkipsWarmUpEntirely() {
        CountingFactory a = new CountingFactory(false);

        new KafkaProducerWarmUp(factories("a", a), false).afterSingletonsInstantiated();

        assertThat(a.calls.get()).isZero();
    }

    /** Ordered, so "failing" is warmed before "healthy" in the failure test. */
    private static Map<String, ProducerFactory<?, ?>> factories(Object... nameThenFactory) {
        Map<String, ProducerFactory<?, ?>> map = new LinkedHashMap<>();
        for (int i = 0; i < nameThenFactory.length; i += 2) {
            map.put((String) nameThenFactory[i], (ProducerFactory<?, ?>) nameThenFactory[i + 1]);
        }
        return map;
    }

    private static final class CountingFactory implements ProducerFactory<String, Object> {
        private final AtomicInteger calls = new AtomicInteger();
        private final boolean fail;

        private CountingFactory(boolean fail) {
            this.fail = fail;
        }

        @Override
        public Producer<String, Object> createProducer() {
            calls.incrementAndGet();
            if (fail) {
                // What an unresolvable bootstrap.servers surfaces as.
                throw new org.apache.kafka.common.config.ConfigException(
                        "No resolvable bootstrap urls given in bootstrap.servers");
            }
            return null;
        }
    }
}
