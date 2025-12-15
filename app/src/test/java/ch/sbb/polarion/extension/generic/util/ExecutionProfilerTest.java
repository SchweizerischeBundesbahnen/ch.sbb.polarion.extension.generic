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
