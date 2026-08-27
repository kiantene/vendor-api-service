package com.nextgen.gameaggregator.core.concurrent;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the fix for ONEAPI-834: pool workers must not inherit the submitting thread's
 * context classloader.
 */
class AppThreadFactoryTest {

    @Test
    void pinsApplicationClassLoaderEvenWhenSubmitterHasAForeignOne() throws Exception {
        ClassLoader expected = AppThreadFactory.class.getClassLoader();
        ClassLoader foreign = new URLClassLoader("foreign", new URL[0], ClassLoader.getPlatformClassLoader());

        ExecutorService pool = Executors.newFixedThreadPool(1, AppThreadFactory.named("test-pool"));
        try {
            // Mimic a ForkJoinPool.commonPool worker submitting the first task: the worker thread
            // is created by this thread and would otherwise inherit its context classloader.
            Thread submitter = new Thread(() -> {
                Thread.currentThread().setContextClassLoader(foreign);
                try {
                    ClassLoader inWorker = pool.submit(
                            () -> Thread.currentThread().getContextClassLoader()).get();
                    assertThat(inWorker)
                            .as("worker must carry the application classloader, not the submitter's")
                            .isSameAs(expected)
                            .isNotSameAs(foreign);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            submitter.start();
            submitter.join();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void threadsAreNonDaemonAndNamed() {
        Thread t = AppThreadFactory.named("api-request-log").newThread(() -> { });

        // Daemon workers would be killed mid-send at JVM exit — see ONEAPI-834 Fix 2.
        assertThat(t.isDaemon()).isFalse();
        assertThat(t.getName()).isEqualTo("api-request-log-1");
        assertThat(t.getContextClassLoader()).isSameAs(AppThreadFactory.class.getClassLoader());
    }
}
