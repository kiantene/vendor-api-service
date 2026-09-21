package com.nextgen.gameaggregator.data.kafka.config;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the fix for ONEAPI-834.
 *
 * <p>{@code KafkaJsonSchemaSerializerConfig}'s static initialiser resolves the
 * {@code context.name.strategy} default through the <em>thread context classloader</em>. In a
 * Spring Boot fat jar the Confluent classes live in {@code BOOT-INF/lib}, invisible to the system
 * classloader, so initialising the class on a thread that carries the system classloader throws —
 * and the JVM then marks the class permanently erroneous for the whole JVM.
 *
 * <p>The scenario cannot be observed in-process: once a class is erroneous it stays erroneous in
 * that classloader. Each case therefore runs in its own isolated {@link URLClassLoader}.
 */
class KafkaSerializerClassInitTest {

    private static final String CONFIG_CLASS =
            "io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig";

    @Test
    void kafkaProducerConfigInitialisesTheSerializerConfigClassAtStartup() {
        // If this fails, the @PostConstruct guard was deleted — see the Javadoc on that method.
        List<Method> hooks = Arrays.stream(KafkaProducerConfig.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostConstruct.class))
                .toList();

        assertThat(hooks)
                .as("KafkaProducerConfig must eagerly initialise the serializer config class on main")
                .isNotEmpty();
    }

    @Test
    void initialisingOnABlindContextClassLoaderPoisonsTheClassForTheWholeJvm() throws Exception {
        try (URLClassLoader appLoader = isolatedAppLoader()) {
            Throwable first = initFrom(appLoader, blindLoader());
            assertThat(first)
                    .as("a blind TCCL must break the static initialiser")
                    .isInstanceOf(ExceptionInInitializerError.class);

            Throwable second = initFrom(appLoader, appLoader);
            assertThat(second)
                    .as("the class stays erroneous afterwards, even from a healthy thread")
                    .isInstanceOf(NoClassDefFoundError.class);
        }
    }

    @Test
    void initialisingOnMainFirstMakesLaterBlindThreadsHarmless() throws Exception {
        try (URLClassLoader appLoader = isolatedAppLoader()) {
            // This is what the @PostConstruct hook does at startup.
            assertThat(initFrom(appLoader, appLoader)).isNull();

            assertThat(initFrom(appLoader, blindLoader()))
                    .as("already-initialised class must not be affected by a blind TCCL")
                    .isNull();
        }
    }

    /**
     * A classloader that cannot see the Confluent jars.
     *
     * <p>In production this role is played by the JVM's <em>system</em> classloader, which in a
     * fat jar cannot see {@code BOOT-INF/lib} — and which is what every
     * {@code ForkJoinPool.commonPool} worker carries as its context classloader. Under surefire the
     * system classloader owns the whole test classpath and is therefore not blind, so the blind
     * loader is modelled explicitly here.
     */
    private static ClassLoader blindLoader() {
        return new URLClassLoader("blind", new URL[0], ClassLoader.getPlatformClassLoader());
    }

    /** A classloader that owns the dependency jars, mimicking Boot's {@code LaunchedURLClassLoader}. */
    private static URLClassLoader isolatedAppLoader() throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }
        // Parent is the platform loader, so the system loader cannot see these classes.
        return new URLClassLoader("boot-inf", urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    }

    /** Initialises the config class on a fresh thread with the given TCCL; returns the failure, or null. */
    private static Throwable initFrom(ClassLoader appLoader, ClassLoader contextClassLoader) throws Exception {
        Throwable[] caught = new Throwable[1];
        Thread t = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            try {
                Class.forName(CONFIG_CLASS, true, appLoader);
            } catch (Throwable e) {
                caught[0] = e;
            }
        });
        t.start();
        t.join();
        return caught[0];
    }
}
