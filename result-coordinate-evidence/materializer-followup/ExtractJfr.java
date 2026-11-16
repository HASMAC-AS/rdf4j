import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/** Streaming diagnostic extraction; no application classes are loaded. */
public final class ExtractJfr {
    private static final Set<String> SELECTED = Set.of(
            "jdk.ExecutionSample", "jdk.NativeMethodSample", "jdk.CPUTimeSample", "jdk.CPUTimeSamplesLost",
            "jdk.ObjectAllocationSample", "jdk.FileWrite", "jdk.Compilation", "jdk.CompilationFailure",
            "jdk.JavaMonitorEnter", "jdk.JavaMonitorWait", "jdk.ThreadPark", "jdk.ThreadCPULoad",
            "jdk.GarbageCollection", "jdk.GCPhasePause", "jdk.CodeCacheFull", "jdk.DataLoss");

    public static void main(String[] args) throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        try (RecordingFile input = new RecordingFile(Path.of(args[0]));
                BufferedWriter output = Files.newBufferedWriter(Path.of(args[1]), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW)) {
            while (input.hasMoreEvents()) {
                RecordedEvent event = input.readEvent();
                String type = event.getEventType().getName();
                counts.merge(type, 1L, Long::sum);
                if (!SELECTED.contains(type)) continue;
                StringBuilder line = new StringBuilder(2048);
                line.append("{\"type\":"); quote(line, type);
                line.append(",\"startEpochNs\":").append(epoch(event.getStartTime()));
                line.append(",\"endEpochNs\":").append(epoch(event.getEndTime()));
                line.append(",\"durationNs\":").append(event.getDuration().toNanos());
                RecordedThread thread = event.hasField("sampledThread")
                        ? event.getThread("sampledThread") : event.getThread();
                if (thread != null) {
                    line.append(",\"thread\":"); quote(line, thread.getJavaName());
                    line.append(",\"javaThreadId\":").append(thread.getJavaThreadId());
                    line.append(",\"osThreadId\":").append(thread.getOSThreadId());
                }
                for (String field : List.of("path", "state", "compiler", "name", "cause", "message")) {
                    if (event.hasField(field)) {
                        line.append(','); quote(line, field); line.append(':');
                        quote(line, String.valueOf((Object) event.getValue(field)));
                    }
                }
                for (String field : List.of("bytesWritten", "weight", "lostSamples", "compileLevel",
                        "codeSize", "inlinedBytes", "user", "system", "failed", "biased", "succeeded", "isOsr")) {
                    if (event.hasField(field)) {
                        line.append(','); quote(line, field); line.append(':').append(String.valueOf((Object) event.getValue(field)));
                    }
                }
                if (event.hasField("samplingPeriod")) {
                    line.append(",\"samplingPeriodNs\":").append(event.getDuration("samplingPeriod").toNanos());
                }
                if (event.hasField("objectClass")) {
                    line.append(",\"objectClass\":"); quote(line, event.getClass("objectClass").getName());
                }
                if (event.hasField("method")) {
                    RecordedMethod method = event.getValue("method");
                    if (method != null) {
                        line.append(",\"method\":"); quote(line, method.getType().getName() + "." + method.getName());
                    }
                }
                RecordedStackTrace trace = event.getStackTrace();
                if (trace != null) {
                    line.append(",\"truncated\":").append(trace.isTruncated());
                    line.append(",\"frames\":[");
                    boolean first = true;
                    for (RecordedFrame frame : trace.getFrames()) {
                        if (!first) line.append(',');
                        first = false;
                        line.append('{').append("\"method\":");
                        RecordedMethod method = frame.getMethod();
                        quote(line, method.getType().getName() + "." + method.getName());
                        line.append(",\"line\":").append(frame.getLineNumber());
                        line.append(",\"bci\":").append(frame.getBytecodeIndex());
                        line.append(",\"kind\":"); quote(line, frame.getType());
                        line.append('}');
                    }
                    line.append(']');
                }
                line.append('}');
                output.write(line.toString()); output.newLine();
            }
        }
        try (BufferedWriter output = Files.newBufferedWriter(Path.of(args[2]), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            StringBuilder json = new StringBuilder("{\n");
            boolean first = true;
            for (Map.Entry<String, Long> count : counts.entrySet()) {
                if (!first) json.append(",\n");
                first = false;
                json.append("  "); quote(json, count.getKey()); json.append(": ").append(count.getValue());
            }
            output.write(json.append("\n}\n").toString());
        }
    }

    private static long epoch(Instant time) {
        return time.getEpochSecond() * 1_000_000_000L + time.getNano();
    }

    private static void quote(StringBuilder output, String value) {
        if (value == null) { output.append("null"); return; }
        output.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> { if (c < 32) output.append(String.format("\\u%04x", (int) c)); else output.append(c); }
            }
        }
        output.append('"');
    }
}
