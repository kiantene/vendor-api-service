package com.nextgen.gameaggregator.core.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that pins the context classloader to the application classloader.
 *
 * <p>Why this exists (ONEAPI-834): {@link java.util.concurrent.ThreadPoolExecutor} creates its
 * workers lazily, on whichever thread happens to call {@code submit()} first, and the new thread
 * inherits that caller's context classloader. A {@link java.util.concurrent.ForkJoinPool}
 * common-pool worker carries {@code ClassLoader.getSystemClassLoader()} — it does not inherit —
 * and in a Spring Boot fat jar the system classloader cannot see {@code BOOT-INF/lib}. A pool
 * worker born on a common-pool thread therefore cannot resolve any class loaded reflectively via
 * the context classloader, which is how Confluent's serializer config resolves
 * {@code context.name.strategy}. That failure poisons the config class for the entire JVM.
 *
 * <p>Pinning the context classloader here makes worker threads independent of who created them.
 *
 * <p>Threads are <strong>non-daemon</strong> at {@code NORM_PRIORITY}, matching
 * {@link java.util.concurrent.Executors#defaultThreadFactory()} — which is what these pools used
 * before this class existed, so the semantics are unchanged. Both must be set <em>explicitly</em>
 * (ONEAPI-859): {@code new Thread(runnable, name)} <em>inherits</em> daemon status and priority
 * from the creating thread, and the creating thread is whoever submitted the first task. In the
 * very scenario this class exists for that submitter is a {@code ForkJoinPool.commonPool} worker,
 * which <em>is</em> a daemon thread — so without these two lines the factory silently produces
 * daemon workers, and in-flight {@code api_request_log} sends and wallet retries are killed at
 * JVM exit.
 */
public final class AppThreadFactory implements ThreadFactory {

    private static final ClassLoader APP_CLASSLOADER = AppThreadFactory.class.getClassLoader();

    private final String namePrefix;
    private final AtomicInteger sequence = new AtomicInteger();

    public AppThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public static AppThreadFactory named(String namePrefix) {
        return new AppThreadFactory(namePrefix);
    }

    /**
     * The application classloader, for the handful of thread factories that cannot use this class
     * because they are deliberately daemon (see {@code LogContextService},
     * {@code KafkaDlqService}) and only need the context classloader pinned.
     */
    public static ClassLoader appClassLoader() {
        return APP_CLASSLOADER;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, namePrefix + "-" + sequence.incrementAndGet());
        thread.setContextClassLoader(APP_CLASSLOADER);
        // Never inherit from the submitting thread — see the class Javadoc.
        thread.setDaemon(false);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}
