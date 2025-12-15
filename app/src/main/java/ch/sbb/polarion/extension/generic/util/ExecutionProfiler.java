package ch.sbb.polarion.extension.generic.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A generic execution profiler for measuring and reporting timing of operations.
 * <p>
 * Usage example:
 * <pre>
 * ExecutionProfiler profiler = new ExecutionProfiler();
 *
 * // Using try-with-resources for automatic timing
 * try (var timer = profiler.startTimer("Database query")) {
 *     executeQuery();
 *     timer.withDetails("fetched 100 rows");
 * }
 *
 * // Using lambda for timing
 * String result = profiler.timed("Processing", () -> processData());
 *
 * // Nested timing
 * try (var outer = profiler.startTimer("Outer operation")) {
 *     doWork1();
 *     try (var inner = profiler.startTimer("Inner operation")) {
 *         doWork2();
 *     }
 * }
 *
 * profiler.finish();
 * System.out.println(profiler.generateReport("My Operation"));
 * </pre>
 */
public class ExecutionProfiler {

    private static final int BAR_WIDTH = 40;
    private static final char BAR_CHAR = '\u2588'; // █
    private static final char BAR_EMPTY = '\u2591'; // ░

    private final StringBuilder logBuilder = new StringBuilder();
    private final List<TimingEntry> timingEntries = new ArrayList<>();
    private final Deque<String> timerStack = new ArrayDeque<>();
    private final long startTime;

    @Getter
    private long totalDurationMs;

    public ExecutionProfiler() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Logs a message with timestamp.
     */
    public void log(String message) {
        logBuilder.append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
                .append(" ")
                .append(message)
                .append(System.lineSeparator());
    }

    /**
     * Records timing for a completed stage.
     */
    public void recordTiming(@NotNull String stageName, long durationMs) {
        recordTiming(stageName, durationMs, null);
    }

    /**
     * Records timing for a completed stage with additional details.
     */
    public void recordTiming(@NotNull String stageName, long durationMs, @Nullable String details) {
        int depth = timerStack.size();
        String parentStage = timerStack.isEmpty() ? null : timerStack.peek();
        timingEntries.add(new TimingEntry(stageName, durationMs, details, depth, parentStage));

        String indent = "  ".repeat(depth);
        String message = details != null
                ? String.format("%s%s completed in %d ms (%s)", indent, stageName, durationMs, details)
                : String.format("%s%s completed in %d ms", indent, stageName, durationMs);
        log(message);
    }

    /**
     * Marks profiling as finished and calculates total duration.
     */
    public void finish() {
        this.totalDurationMs = System.currentTimeMillis() - startTime;
    }

    /**
     * Returns the raw log output.
     */
    public String getLog() {
        return logBuilder.toString();
    }

    /**
     * Returns a copy of all timing entries.
     */
    public List<TimingEntry> getTimingEntries() {
        return new ArrayList<>(timingEntries);
    }

    /**
     * Starts a new timer for the given stage name.
     * Use with try-with-resources for automatic timing.
     */
    public Timer startTimer(@NotNull String stageName) {
        return new Timer(this, stageName);
    }

    /**
     * Executes a supplier and records timing.
     */
    public <T> T timed(@NotNull String stageName, @NotNull Supplier<T> supplier) {
        try (Timer ignored = startTimer(stageName)) {
            return supplier.get();
        }
    }

    /**
     * Executes a supplier and records timing with details derived from the result.
     */
    public <T> T timed(@NotNull String stageName, @NotNull Supplier<T> supplier, @NotNull Function<T, String> detailsProvider) {
        try (Timer timer = startTimer(stageName)) {
            T result = supplier.get();
            timer.withDetails(detailsProvider.apply(result));
            return result;
        }
    }

    /**
     * Executes a runnable and records timing.
     */
    public void timed(@NotNull String stageName, @NotNull Runnable runnable) {
        try (Timer ignored = startTimer(stageName)) {
            runnable.run();
        }
    }

    /**
     * Generates a timing report.
     *
     * @param title the title for the report
     * @return formatted timing report
     */
    public String generateReport(@Nullable String title) {
        StringBuilder report = new StringBuilder();

        appendHeader(report, title);
        appendTimingBreakdown(report);
        appendSlowestStages(report);
        appendTimeline(report);
        appendDetailedLog(report);

        return report.toString();
    }

    protected void appendHeader(StringBuilder report, @Nullable String title) {
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("EXECUTION TIMING REPORT").append(System.lineSeparator());
        report.append("=".repeat(80)).append(System.lineSeparator());
        if (title != null) {
            report.append("Title: ").append(title).append(System.lineSeparator());
        }
        report.append("Generated: ").append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)).append(System.lineSeparator());
        report.append("Total Duration: ").append(formatDuration(totalDurationMs)).append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    protected void appendTimingBreakdown(StringBuilder report) {
        report.append("TIMING BREAKDOWN:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());
        report.append(String.format("%-40s %10s %7s  %s%n", "Stage", "Duration", "Percent", "Visual"));
        report.append("-".repeat(80)).append(System.lineSeparator());

        List<TimingEntry> orderedEntries = getOrderedEntries();

        long accountedTime = 0;
        for (TimingEntry entry : orderedEntries) {
            double percent = totalDurationMs > 0 ? (entry.durationMs() * 100.0 / totalDurationMs) : 0;
            String indent = "  ".repeat(entry.depth());
            String stageName = truncate(indent + entry.stageName(), 40);
            String bar = createBar(percent);

            report.append(String.format("%-40s %7d ms %6.1f%%  %s%n", stageName, entry.durationMs(), percent, bar));
            if (entry.details() != null) {
                report.append(String.format("  %s\u2514\u2500 %s%n", "  ".repeat(entry.depth()), entry.details()));
            }
            if (entry.depth() == 0) {
                accountedTime += entry.durationMs();
            }
        }

        long unaccountedTime = totalDurationMs - accountedTime;
        if (unaccountedTime > 0 && totalDurationMs > 0) {
            double percent = unaccountedTime * 100.0 / totalDurationMs;
            String bar = createBar(percent);
            report.append(String.format("%-40s %7d ms %6.1f%%  %s%n", "(other/overhead)", unaccountedTime, percent, bar));
        }

        report.append("-".repeat(80)).append(System.lineSeparator());
        report.append(String.format("%-40s %7d ms %6.1f%%%n", "TOTAL", totalDurationMs, 100.0));
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    protected List<TimingEntry> getOrderedEntries() {
        List<TimingEntry> result = new ArrayList<>();
        Map<String, List<TimingEntry>> childrenMap = new LinkedHashMap<>();

        for (TimingEntry entry : timingEntries) {
            String parent = entry.parentStage();
            childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(entry);
        }

        List<TimingEntry> rootEntries = childrenMap.getOrDefault(null, new ArrayList<>());
        for (TimingEntry root : rootEntries) {
            addEntryWithChildren(root, childrenMap, result);
        }

        return result;
    }

    private void addEntryWithChildren(TimingEntry entry, Map<String, List<TimingEntry>> childrenMap, List<TimingEntry> result) {
        result.add(entry);
        List<TimingEntry> children = childrenMap.get(entry.stageName());
        if (children != null) {
            for (TimingEntry child : children) {
                addEntryWithChildren(child, childrenMap, result);
            }
        }
    }

    protected void appendSlowestStages(StringBuilder report) {
        if (timingEntries.isEmpty()) {
            return;
        }

        report.append("SLOWEST STAGES (potential bottlenecks):").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        timingEntries.stream()
                .sorted(Comparator.comparingLong(TimingEntry::durationMs).reversed())
                .limit(5)
                .forEach(entry -> {
                    double percent = totalDurationMs > 0 ? (entry.durationMs() * 100.0 / totalDurationMs) : 0;
                    String indicator = getPerformanceIndicator(percent);
                    report.append(String.format("  %s %s (%s, %.1f%%)%n",
                            indicator, entry.stageName(), formatDuration(entry.durationMs()), percent));
                    if (entry.details() != null) {
                        report.append(String.format("       \u2514\u2500 %s%n", entry.details()));
                    }
                });

        report.append(System.lineSeparator());
        report.append("Legend: \uD83D\uDD34 >30%  \uD83D\uDFE1 15-30%  \uD83D\uDFE2 <15%").append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    protected void appendTimeline(StringBuilder report) {
        if (timingEntries.isEmpty()) {
            return;
        }

        report.append("EXECUTION TIMELINE:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        long cumulative = 0;
        for (TimingEntry entry : timingEntries) {
            if (entry.depth() == 0) {
                double startPercent = totalDurationMs > 0 ? (cumulative * 100.0 / totalDurationMs) : 0;
                double endPercent = totalDurationMs > 0 ? ((cumulative + entry.durationMs()) * 100.0 / totalDurationMs) : 0;
                String timeline = createTimeline(startPercent, endPercent);
                report.append(String.format("  %-30s %s%n", truncate(entry.stageName(), 30), timeline));
                cumulative += entry.durationMs();
            }
        }

        report.append("  ").append("0%").append(" ".repeat(36)).append("50%").append(" ".repeat(35)).append("100%").append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    protected void appendDetailedLog(StringBuilder report) {
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("DETAILED LOG:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());
        report.append(logBuilder);
    }

    protected String createBar(double percent) {
        int filled = (int) Math.round(percent / 100.0 * BAR_WIDTH);
        filled = Math.min(filled, BAR_WIDTH);
        return String.valueOf(BAR_CHAR).repeat(filled) + String.valueOf(BAR_EMPTY).repeat(BAR_WIDTH - filled);
    }

    protected String createTimeline(double startPercent, double endPercent) {
        int start = (int) Math.round(startPercent / 100.0 * BAR_WIDTH);
        int end = (int) Math.round(endPercent / 100.0 * BAR_WIDTH);
        start = Math.max(0, Math.min(start, BAR_WIDTH));
        end = Math.max(start, Math.min(end, BAR_WIDTH));

        return "\u00B7".repeat(start) + "\u2588".repeat(end - start) + "\u00B7".repeat(BAR_WIDTH - end);
    }

    protected String getPerformanceIndicator(double percent) {
        if (percent > 30) return "\uD83D\uDD34";
        if (percent > 15) return "\uD83D\uDFE1";
        return "\uD83D\uDFE2";
    }

    /**
     * Formats duration in human-readable form.
     */
    public static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format("%.1f sec", ms / 1000.0);
        } else {
            return String.format("%.1f min", ms / 60000.0);
        }
    }

    /**
     * Formats size in human-readable form.
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    protected String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Returns the timer stack for subclasses.
     */
    protected Deque<String> getTimerStack() {
        return timerStack;
    }

    /**
     * A single timing entry recording the duration of a stage.
     */
    public record TimingEntry(String stageName, long durationMs, String details, int depth, String parentStage) {
        public TimingEntry(String stageName, long durationMs, String details) {
            this(stageName, durationMs, details, 0, null);
        }
    }

    /**
     * Auto-closeable timer for measuring execution time.
     */
    public static class Timer implements AutoCloseable {
        private final ExecutionProfiler profiler;
        private final String stageName;
        private final long startTime;
        private String details;

        public Timer(@NotNull ExecutionProfiler profiler, @NotNull String stageName) {
            this.profiler = profiler;
            this.stageName = stageName;
            this.startTime = System.currentTimeMillis();
            profiler.getTimerStack().push(stageName);
        }

        public Timer withDetails(String details) {
            this.details = details;
            return this;
        }

        @Override
        public void close() {
            profiler.getTimerStack().pop();
            long duration = System.currentTimeMillis() - startTime;
            profiler.recordTiming(stageName, duration, details);
        }
    }
}
