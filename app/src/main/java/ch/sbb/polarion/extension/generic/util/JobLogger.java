package ch.sbb.polarion.extension.generic.util;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class JobLogger {

    private final List<String> messages = new ArrayList<>();

    private JobLogger() {
    }

    public void log(String message) {
        messages.add(message);
    }

    public void log(String message, Object... args) {
        messages.add(String.format(message, args));
    }

    public void log(Exception exception) {
        List<String> stackTraceLines = new ArrayList<>();
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTraceLines.add(stackTraceElement.toString());
        }
        messages.addAll(stackTraceLines);
    }

    public void separator() {
        messages.add("-------------------------------------------------");
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getLog() {
        return String.join(System.lineSeparator(), messages);
    }

    public void clear() {
        messages.clear();
    }

    public static JobLogger getInstance() {
        return JobLoggerHolder.INSTANCE;
    }

    private static class JobLoggerHolder {
        private static final JobLogger INSTANCE = new JobLogger();
    }
}
