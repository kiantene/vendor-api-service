package com.nextgen.gameaggregator.core.concurrent;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
        AtomicReference<ClassLoader> inWorker = new AtomicReference<>();
        try {
            // Mimic a ForkJoinPool.commonPool worker submitting the first task: the worker thread
            // is created by this thread and would otherwise inherit its context classloader.
            //
            // ONEAPI-859: capture here, assert on the test thread. An AssertionError raised inside
            // this Runnable is an Error, not an Exception — it escapes catch (Exception), kills the
            // submitter silently, and join() still returns normally, so JUnit never sees it. The
            // earlier version of this test asserted inside the thread and passed with
            // setContextClassLoader deleted from AppThreadFactory.
            Thread submitter = new Thread(() -> {
                Thread.currentThread().setContextClassLoader(foreign);
                try {
                    inWorker.set(pool.submit(
                            () -> Thread.currentThread().getContextClassLoader()).get());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            submitter.start();
            submitter.join();
        } finally {
            pool.shutdownNow();
        }

        assertThat(inWorker.get())
                .as("worker must carry the application classloader, not the submitter's")
                .isSameAs(expected)
                .isNotSameAs(foreign);
    }

    @Test
    void threadsAreNonDaemonAndNamed() {
        Thread t = AppThreadFactory.named("api-request-log").newThread(() -> { });

        // Daemon workers would be killed mid-send at JVM exit — see ONEAPI-834 Fix 2.
        assertThat(t.isDaemon()).isFalse();
        assertThat(t.getName()).isEqualTo("api-request-log-1");
        assertThat(t.getContextClassLoader()).isSameAs(AppThreadFactory.class.getClassLoader());
    }

    /**
     * ONEAPI-859. The assertion above is not enough on its own: it creates the thread from the
     * JUnit thread, which is already non-daemon at NORM_PRIORITY, so it passes even when the
     * factory sets neither. {@code new Thread(runnable, name)} <em>inherits</em> both from the
     * creating thread, and pool workers are created by whoever submits the first task — which in
     * the ONEAPI-834 scenario is a {@code ForkJoinPool.commonPool} worker, itself a daemon.
     *
     * <p>The submitter is made daemon explicitly rather than relying on the common pool, so the
     * test does not depend on common-pool sizing: on a 1-vCPU agent parallelism collapses and
     * {@code CompletableFuture} falls back to a caller-derived thread that is not a daemon.
     */
    @Test
    void threadsAreNonDaemonEvenWhenCreatedFromADaemonSubmitter() throws Exception {
        AtomicReference<Thread> created = new AtomicReference<>();

        Thread daemonSubmitter = new Thread(() ->
                created.set(AppThreadFactory.named("t").newThread(() -> { })));
        daemonSubmitter.setDaemon(true);
        daemonSubmitter.setPriority(Thread.MIN_PRIORITY);
        daemonSubmitter.start();
        daemonSubmitter.join();

        assertThat(created.get().isDaemon())
                .as("worker must not inherit the submitter's daemon flag")
                .isFalse();
        assertThat(created.get().getPriority())
                .as("worker must not inherit the submitter's priority")
                .isEqualTo(Thread.NORM_PRIORITY);
        assertThat(created.get().getContextClassLoader())
                .isSameAs(AppThreadFactory.class.getClassLoader());
    }
}
