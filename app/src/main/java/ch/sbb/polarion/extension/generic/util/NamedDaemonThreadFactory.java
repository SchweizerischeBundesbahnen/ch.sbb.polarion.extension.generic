package ch.sbb.polarion.extension.generic.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that names its threads and makes them daemon, so they do not keep the JVM alive.
 */
public class NamedDaemonThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger(1);
    private final String namePrefix;

    public NamedDaemonThreadFactory(@NotNull String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(@NotNull Runnable runnable) {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setDaemon(true);
        thread.setName("%s-%s".formatted(namePrefix, counter.getAndIncrement()));
        return thread;
    }
}
