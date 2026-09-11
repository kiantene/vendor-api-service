package com.nextgen.gameaggregator.data.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Realises every Kafka producer during startup, on the context-refresh thread, whose context
 * classloader is Spring Boot's {@code LaunchedURLClassLoader}.
 *
 * <p>ONEAPI-834: producers are otherwise built lazily on the first {@code send()}, by whichever
 * thread gets there first. Confluent's serializer config resolves its {@code context.name.strategy}
 * default through the <em>thread context</em> classloader. A thread created by a
 * {@code ForkJoinPool.commonPool} worker carries the system classloader, which cannot see
 * {@code BOOT-INF/lib} in a fat jar. That static initialiser then throws, the JVM marks the class
 * permanently erroneous, and every producer for every topic fails until restart. Building the
 * producers here removes the race rather than narrowing it, and covers the whole Confluent
 * initialisation chain rather than a single class.
 *
 * <p><strong>Warm-up failure is logged, not fatal (ONEAPI-859).</strong> A failure here is almost
 * always {@code bootstrap.servers} not resolving at boot, and refusing to start would take the
 * whole fleet down on a DNS blip — for a pipeline that carries logging, history and wallet raw
 * data, not the bet or launch path. The protection does not need the abort: the serializer is
 * configured, and the Confluent classes are therefore initialised on this thread, <em>before</em>
 * bootstrap addresses are resolved. A warm-up that throws has still done its job. Verified in
 * ONEAPI-859: warm-up against an unresolvable broker throws, and a later producer built on a
 * blind-context-classloader thread still succeeds.
 *
 * <p>{@link SmartInitializingSingleton} runs after all singletons exist but before
 * {@code ContextRefreshedEvent}, which is what starts {@code @Scheduled} tasks, so this is strictly
 * ordered before the first scheduler tick. Do <strong>not</strong> convert this to an
 * {@code ApplicationRunner} or {@code CommandLineRunner}: those run after refresh, concurrent with
 * an already-started scheduler, and the ordering guarantee is lost.
 */
@Slf4j
@Component
public class KafkaProducerWarmUp implements SmartInitializingSingleton {

    private final List<ProducerFactory<?, ?>> producerFactories;
    private final boolean enabled;

    public KafkaProducerWarmUp(List<ProducerFactory<?, ?>> producerFactories,
                               @Value("${kafka.producer.warm-up.enabled:true}") boolean enabled) {
        this.producerFactories = producerFactories;
        this.enabled = enabled;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!enabled) {
            log.warn("Kafka producer warm-up disabled (kafka.producer.warm-up.enabled=false); "
                    + "producers will be built lazily on first send — see ONEAPI-834");
            return;
        }
        for (ProducerFactory<?, ?> factory : producerFactories) {
            try {
                // The factory caches the shared producer and closes it via destroy() — not closed here.
                factory.createProducer();
                log.info("Kafka producer warmed up: {}", factory.getClass().getSimpleName());
            } catch (Exception e) {
                // Deliberately not rethrown — see the class Javadoc.
                log.error("Kafka producer warm-up failed for {}; the serializer classes are still "
                        + "initialised on this thread, so the ONEAPI-834 guard holds. Producer will "
                        + "be retried lazily on first send.", factory.getClass().getSimpleName(), e);
            }
        }
    }
}
