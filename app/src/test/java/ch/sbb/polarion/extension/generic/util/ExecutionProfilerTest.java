package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionProfilerTest {

    @Test
    void testBasicTiming() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        profiler.timed("Test operation", () -> clock.advance(50));
        profiler.finish();

        List<ExecutionProfiler.TimingEntry> entries = profiler.getTimingEntries();
        assertEquals(1, entries.size());
        assertEquals("Test operation", entries.get(0).stageName());
        assertEquals(50, entries.get(0).durationMs());
    }

    @Test
    void testTimedWithResult() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        String result = profiler.timed("Compute value", () -> {
            clock.advance(10);
            return "computed";
        });
        profiler.finish();

        assertEquals("computed", result);
        assertEquals(1, profiler.getTimingEntries().size());
        assertEquals(10, profiler.getTimingEntries().get(0).durationMs());
    }

    @Test
    void testTimedWithDetails() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        int result = profiler.timed("Process items", () -> {
            clock.advance(10);
            return 42;
        }, count -> "processed " + count + " items");
        profiler.finish();

        assertEquals(42, result);
        assertEquals("processed 42 items", profiler.getTimingEntries().get(0).details());
        assertEquals(10, profiler.getTimingEntries().get(0).durationMs());
    }

    @Test
    void testNestedTimers() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        try (var outer = profiler.startTimer("Outer")) {
            clock.advance(10);
            try (var inner = profiler.startTimer("Inner")) {
                clock.advance(20);
            }
        }
        profiler.finish();

        List<ExecutionProfiler.TimingEntry> entries = profiler.getTimingEntries();
        assertEquals(2, entries.size());

        // Inner completes first
        assertEquals("Inner", entries.get(0).stageName());
        assertEquals(1, entries.get(0).depth());
        assertEquals("Outer", entries.get(0).parentStage());
        assertEquals(20, entries.get(0).durationMs());

        // Outer completes second
        assertEquals("Outer", entries.get(1).stageName());
        assertEquals(0, entries.get(1).depth());
        assertNull(entries.get(1).parentStage());
        assertEquals(30, entries.get(1).durationMs()); // 10 + 20
    }

    @Test
    void testTimerWithDetails() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        try (var timer = profiler.startTimer("Database query")) {
            clock.advance(10);
            timer.withDetails("fetched 100 rows");
        }
        profiler.finish();

        assertEquals("fetched 100 rows", profiler.getTimingEntries().get(0).details());
        assertEquals(10, profiler.getTimingEntries().get(0).durationMs());
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
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        profiler.timed("Step 1", () -> clock.advance(30));
        profiler.timed("Step 2", () -> clock.advance(20));
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
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);
        profiler.finish();

        assertEquals(0, profiler.getTotalDurationMs());
        assertTrue(profiler.getTimingEntries().isEmpty());

        String report = profiler.generateReport(null);
        assertTrue(report.contains("EXECUTION TIMING REPORT"));
    }

    @Test
    void testTotalDuration() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        profiler.timed("Step 1", () -> clock.advance(100));
        profiler.timed("Step 2", () -> clock.advance(50));
        clock.advance(25); // unaccounted time
        profiler.finish();

        assertEquals(175, profiler.getTotalDurationMs());
    }

    @Test
    void testRecordTimingDirectly() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        profiler.recordTiming("Manual stage", 100);
        profiler.recordTiming("Manual stage with details", 200, "some details");
        profiler.finish();

        List<ExecutionProfiler.TimingEntry> entries = profiler.getTimingEntries();
        assertEquals(2, entries.size());
        assertEquals("Manual stage", entries.get(0).stageName());
        assertEquals(100, entries.get(0).durationMs());
        assertNull(entries.get(0).details());
        assertEquals("Manual stage with details", entries.get(1).stageName());
        assertEquals(200, entries.get(1).durationMs());
        assertEquals("some details", entries.get(1).details());
    }

    @Test
    void testReportWithDetails() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        try (var timer = profiler.startTimer("Stage with details")) {
            clock.advance(50);
            timer.withDetails("processed 100 items");
        }
        profiler.finish();

        String report = profiler.generateReport("Report with Details");
        assertTrue(report.contains("Stage with details"));
        assertTrue(report.contains("processed 100 items"));
        assertTrue(report.contains("└─")); // details indicator
    }

    @Test
    void testReportWithUnaccountedTime() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        profiler.timed("Step 1", () -> clock.advance(30));
        clock.advance(20); // unaccounted overhead
        profiler.finish();

        String report = profiler.generateReport("With Overhead");
        assertTrue(report.contains("(other/overhead)"));
        assertTrue(report.contains("20 ms")); // the unaccounted time
    }

    @Test
    void testReportWithNestedTimersOrdering() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        try (var outer = profiler.startTimer("Parent")) {
            clock.advance(10);
            try (var inner = profiler.startTimer("Child")) {
                clock.advance(20);
            }
        }
        profiler.finish();

        String report = profiler.generateReport("Nested");
        // Verify the report contains both entries with proper indentation
        assertTrue(report.contains("Parent"));
        assertTrue(report.contains("Child"));
    }

    @Test
    void testReportSlowestStagesWithDetails() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        try (var timer = profiler.startTimer("Slow stage")) {
            clock.advance(100);
            timer.withDetails("bottleneck info");
        }
        profiler.finish();

        String report = profiler.generateReport("Slowest");
        assertTrue(report.contains("SLOWEST STAGES"));
        assertTrue(report.contains("Slow stage"));
        assertTrue(report.contains("bottleneck info"));
    }

    @Test
    void testReportTimelineWithMultipleStages() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        profiler.timed("First", () -> clock.advance(40));
        profiler.timed("Second", () -> clock.advance(60));
        profiler.finish();

        String report = profiler.generateReport("Timeline Test");
        assertTrue(report.contains("EXECUTION TIMELINE"));
        assertTrue(report.contains("First"));
        assertTrue(report.contains("Second"));
        assertTrue(report.contains("0%"));
        assertTrue(report.contains("50%"));
        assertTrue(report.contains("100%"));
    }

    @Test
    void testTruncateLongStageName() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        String longName = "This is a very long stage name that should be truncated in the report";
        profiler.timed(longName, () -> clock.advance(10));
        profiler.finish();

        String report = profiler.generateReport("Truncate Test");
        assertTrue(report.contains("...")); // truncation indicator
    }

    @Test
    void testTimingEntryRecordConstructor() {
        ExecutionProfiler.TimingEntry entry = new ExecutionProfiler.TimingEntry("test", 100, "details");
        assertEquals("test", entry.stageName());
        assertEquals(100, entry.durationMs());
        assertEquals("details", entry.details());
        assertEquals(0, entry.depth());
        assertNull(entry.parentStage());
    }

    @Test
    void testPerformanceIndicators() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);

        // Create stages with different percentages to trigger all indicators
        profiler.timed("Critical", () -> clock.advance(50));   // >30% - red
        profiler.timed("Warning", () -> clock.advance(25));    // 15-30% - yellow
        profiler.timed("Normal", () -> clock.advance(10));     // <15% - green
        profiler.finish();

        String report = profiler.generateReport("Indicators");
        assertTrue(report.contains("SLOWEST STAGES"));
        // The report should contain performance indicators
        assertTrue(report.contains("🔴") || report.contains("🟡") || report.contains("🟢"));
    }

    @Test
    void testReportWithZeroTotalDuration() {
        TestClock clock = new TestClock();
        ExecutionProfiler profiler = new ExecutionProfiler(clock);
        // Don't advance clock - total duration will be 0
        profiler.timed("Quick", () -> {}); // no time passes
        profiler.finish();

        assertEquals(0, profiler.getTotalDurationMs());
        String report = profiler.generateReport("Zero Duration");
        assertTrue(report.contains("EXECUTION TIMING REPORT"));
    }

    @Test
    void testDefaultConstructor() {
        ExecutionProfiler profiler = new ExecutionProfiler();
        profiler.timed("Test", () -> {});
        profiler.finish();

        // Should work with system clock
        assertTrue(profiler.getTotalDurationMs() >= 0);
    }

    /**
     * A controllable Clock implementation for testing.
     */
    private static class TestClock extends Clock {
        private final AtomicLong currentTime = new AtomicLong(0);

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(currentTime.get());
        }

        @Override
        public long millis() {
            return currentTime.get();
        }

        public void advance(long millis) {
            currentTime.addAndGet(millis);
        }
    }
}
