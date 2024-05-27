package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobLoggerTest {

    @Test
    void testLogger() {
        JobLogger logger = JobLogger.getInstance();
        logger.log("testMessage");
        RuntimeException testException = new RuntimeException("testException");
        testException.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("testClass1", "testMethod1", "testFileName1", 1),
                new StackTraceElement("testClass2", "testMethod2", "testFileName2", 2),
                new StackTraceElement("testClass3", "testMethod3", "testFileName3", 3)
        });
        logger.log(testException);
        logger.separator();
        logger.log("testMessage with %s", "param");

        assertEquals(6, logger.getMessages().size());
        assertEquals("testMessage", logger.getMessages().get(0));
        assertEquals("testClass1.testMethod1(testFileName1:1)", logger.getMessages().get(1));
        assertEquals("testClass2.testMethod2(testFileName2:2)", logger.getMessages().get(2));
        assertEquals("testClass3.testMethod3(testFileName3:3)", logger.getMessages().get(3));
        assertTrue(logger.getMessages().get(4).matches("-+"));
        assertEquals("testMessage with param", logger.getMessages().get(5));

        logger.clear();
        assertTrue(logger.getMessages().isEmpty());
    }
}