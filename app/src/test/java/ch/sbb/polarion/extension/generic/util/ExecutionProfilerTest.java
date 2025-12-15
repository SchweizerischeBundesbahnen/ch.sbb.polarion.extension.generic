package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionProfilerTest {

    @Test
    void testBasicTiming() {
        ExecutionProfiler profiler = new ExecutionProfiler();

        profiler.timed("Test operation", () -> sleep(50));
        profiler.finish();

        List<ExecutionProfiler.TimingEntry> entries = profiler.getTimingEntries();
        assertEquals(1, entries.size());
        assertEquals("Test operation", entries.get(0).stageName());
        assertTrue(entries.get(0).durationMs() >= 50);
    }

    @Test
    void testTimedWithResult() {
        ExecutionProfiler profiler = new ExecutionProfiler();

        String result = profiler.timed("Compute value", () -> {
            sleep(10);
            return "computed";
        });
        profiler.finish();

        assertEquals("computed", result);
        assertEquals(1, profiler.getTimingEntries().size());
    }

    @Test
    void testTimedWithDetails() {
        ExecutionProfiler profiler = new ExecutionProfiler();

        int result = profiler.timed("Process items", () -> {
            sleep(10);
            return 42;
        }, count -> "processed " + count + " items");
        profiler.finish();

        assertEquals(42, result);
        assertEquals("processed 42 items", profiler.getTimingEntries().get(0).details());
    }

    @Test
    void testNestedTimers() {
        ExecutionProfiler profiler = new ExecutionProfiler();

        try (var outer = profiler.startTimer("Outer")) {
            sleep(10);
            try (var inner = profiler.startTimer("Inner")) {
                sleep(20);
            }
        }
        profiler.finish();

        List<ExecutionProfiler.TimingEntry> entries = profiler.getTimingEntries();
        assertEquals(2, entries.size());

        // Inner completes first
        assertEquals("Inner", entries.get(0).stageName());
        assertEquals(1, entries.get(0).depth());
        assertEquals("Outer", entries.get(0).parentStage());

        // Outer completes second
        assertEquals("Outer", entries.get(1).stageName());
        assertEquals(0, entries.get(1).depth());
        assertNull(entries.get(1).parentStage());
    }

    @Test
    void testTimerWithDetails() {
        ExecutionProfiler profiler = new ExecutionProfiler();

        try (var timer = profiler.startTimer("Database query")) {
            sleep(10);
            timer.withDetails("fetched 100 rows");
        }
        profiler.finish();

        assertEquals("fetched 100 rows", profiler.getTimingEntries().get(0).details());
    }

    @Test
    void testLog() {
        ExecutionProfiler profiler = new ExecutionProfiler();
        profiler.log("Test message");

        String log = profiler.getLog();
        assertTrue(log.contains("Test message"));
    }

    @Test
    void testFormatDuration() {
        assertEquals("500 ms", ExecutionProfiler.formatDuration(500));
        assertEquals("1.5 sec", ExecutionProfiler.formatDuration(1500));
        assertEquals("2.0 min", ExecutionProfiler.formatDuration(120000));
    }

    @Test
    void testFormatSize() {
        assertEquals("500 B", ExecutionProfiler.formatSize(500));
        assertEquals("1.5 KB", ExecutionProfiler.formatSize(1536));
        assertEquals("2.0 MB", ExecutionProfiler.formatSize(2 * 1024 * 1024));
    }

    @Test
    void testGenerateReport() {
        ExecutionProfiler profiler = new ExecutionProfiler();

        profiler.timed("Step 1", () -> sleep(30));
        profiler.timed("Step 2", () -> sleep(20));
        profiler.finish();

        String report = profiler.generateReport("Test Report");

        assertTrue(report.contains("EXECUTION TIMING REPORT"));
        assertTrue(report.contains("Test Report"));
        assertTrue(report.contains("Step 1"));
        assertTrue(report.contains("Step 2"));
        assertTrue(report.contains("TIMING BREAKDOWN"));
        assertTrue(report.contains("SLOWEST STAGES"));
        assertTrue(report.contains("EXECUTION TIMELINE"));
        assertTrue(report.contains("DETAILED LOG"));
    }

    @Test
    void testEmptyProfiler() {
        ExecutionProfiler profiler = new ExecutionProfiler();
        profiler.finish();

        assertEquals(0, profiler.getTotalDurationMs(), 10);
        assertTrue(profiler.getTimingEntries().isEmpty());

        String report = profiler.generateReport(null);
        assertTrue(report.contains("EXECUTION TIMING REPORT"));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
