// src/test/java/dev/badkraft/anvil/parser/FileTest.java
package dev.badkraft.anvil.parser;

import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.core.data.*;
import dev.badkraft.anvil.utilities.AmlMinifier;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class FileTest {
    private static final Path TEST_DIR = Paths.get("build/resources/test");
    private static boolean speedTest = false;

    private static Context currentContext = null;

    private static String testSource = """
#!aml
@[version="1.0"]

player := {
    name   := "Notch"
    health := 20
    pos    := (10, 64, -32) 
    desc   := @md`**bold** and \\`escaped\\``
    realm  := @yaml`
server:
  name: "My Secret Realm"
  host: localhost
  port: 25565
  motd: "Welcome!"`
}
    """;

    public static void main(String[] args) throws IOException {
        ensureTestDirectory();
        List<Path> files = new ArrayList<>();
        if (args.length > 0 && args[0].equals("--speed")) {
            speedTest = true;
            args = Arrays.copyOfRange(args, 1, args.length);
            files = resolveFiles(args);
            if (files.isEmpty()) {
                System.out.println("No .aml files to process.");
                return;
            }
        } else if (args.length > 0 && args[0].equals("--core-tests")) {
            speedTest = true;
            files.add(TEST_DIR.resolve("assignments.aml"));
            files.add(TEST_DIR.resolve("arrays.aml"));
            files.add(TEST_DIR.resolve("objects.aml"));
            files.add(TEST_DIR.resolve("tuples.aml"));
            files.add(TEST_DIR.resolve("attributes.aml"));
            files.add(TEST_DIR.resolve("inherits.aml"));
            files.add(TEST_DIR.resolve("large_block_lib.aml"));
        } else {
            files = resolveFiles(args);
            if (files.isEmpty()) {
                System.out.println("No .aml files to process.");
                return;
            }
        }

        List<TestResult> results = new ArrayList<>();
        String minified = AmlMinifier.minify(testSource);
        TestResult result = runSingleSource(minified);
        results.add(result);
        printResult(result);
        System.out.println();

        System.out.println("Running " + files.size() + " test file(s)...\n");
        for (Path path : files) {
            result = runSingleFile(path);
            results.add(result);
            printResult(result);
            System.out.println();
        }

        long failed = results.stream().filter(r -> !r.success).count();
        System.out.printf("SUMMARY: %d/%d passed (%.2f%%)%n",
                results.size() - failed, results.size(),
                100.0 * (results.size() - failed) / results.size());

        if (failed > 0) {
            System.out.println("FAILURES DETECTED. Fix them.");
            System.exit(1);
        } else {
            System.out.println("All tests passed.");
        }
    }

    private static List<Path> resolveFiles(String[] args) throws IOException {
        if (args.length > 0) {
            Path p = TEST_DIR.resolve(args[0]);
            if (!Files.exists(p)) {
                System.err.println("File not found: " + p);
                System.exit(1);
            }
            return List.of(p);
        }

        try (var stream = Files.list(TEST_DIR)) {
            return stream
                    .filter(p -> p.toString().endsWith(".aml"))
                    .sorted()
                    .toList();
        }
    }

    private static void ensureTestDirectory() {
        if (!Files.exists(TEST_DIR)) {
            System.err.println("Test directory not found: " + TEST_DIR);
            System.err.println("Run ./gradlew processTestResources first.");
            System.exit(1);
        }
    }

    private static TestResult runSingleFile(Path path) {
        String fileName = path.getFileName().toString();
        long fileSizeBytes = 0;
        try {
            fileSizeBytes = Files.size(path);
        } catch (IOException e) {
            // ignore
        }
        System.out.printf("=== %s (%d bytes) ===%n", fileName, fileSizeBytes);

        long start = System.nanoTime();
        Context context;
        boolean success = true;
        try {
            currentContext = context = Context.builder()
                    .source(path)
                    .build();
            AnvilParser.parse(context);
        } catch (Exception e) {
            context = null;
            success = false;
            double parseTimeMs = (System.nanoTime() - start) / 1_000_000.0;
            return new TestResult(fileName, path, null, parseTimeMs, false, e);
        }

        double parseTimeMs = (System.nanoTime() - start) / 1_000_000.0;  // ns → ms
        // success &= context.isParsed();

        return new TestResult(fileName, path, context, parseTimeMs, success, null);
    }

    private static TestResult runSingleSource(String minified) {
        // do what runSingleFile does, but for a string source
        System.out.println("=== In-Memory Test Source ===");
        System.out.println(minified);
        System.out.println("Running " + testSource.length() + "-byte test source...\n");

        long start = System.nanoTime();
        Context context;
        boolean success = true;
        try {
            currentContext = context = Context.builder()
                    .source(minified)
                    .namespace("in.memory.test")
                    .build();
            AnvilParser.parse(context);
        } catch (Exception e) {
            context = null;
            success = false;
            long parseTime = (System.nanoTime() - start) / 1_000_000;
            return new TestResult("In-Memory Test Source", null, null, parseTime, false, e);
        }
        long parseTime = (System.nanoTime() - start) / 1_000_000;
        success &= context.isParsed();
        return new TestResult("In-Memory Test Source", null, context, parseTime, success, null);
    }

    private static void printResult(TestResult r) {
        if (!r.success) {
            System.out.printf("FAILED in %.3f ms%n", r.parseTimeMs);
            if (r.exception != null) {
                System.out.println("Exception: " + r.exception.getMessage());
                r.exception.printStackTrace(System.out);
            }
            return;
        }

        System.out.printf("PASS — %s%n", formatParseTime(r.parseTimeMs));
        if (speedTest || r.context == null) return;

        Context ctx = r.context;
        System.out.println("Module [" + ctx.namespace() + "] dialect=" + ctx.dialect());
        System.out.println();

        if (!ctx.attributes().isEmpty()) {
            System.out.println("Module Attributes:");
            for (Attribute a : ctx.attributes()) {
                System.out.println("  @[ " + formatAttribute(a) + " ]");
            }
            System.out.println();
        }

        if (ctx.statements().isEmpty()) {
            System.out.println("(no statements)");
        } else {
            System.out.println("Statements:");
            for (Statement s : ctx.statements()) {
                System.out.println(prettyPrintStatement(s, 1));
            }
        }
    }

    // === Pretty Printing (updated for new Value design) ===
    private static String prettyPrintStatement(Statement stmt, int indent) {
        String pad = "  ".repeat(indent);
        StringBuilder sb = new StringBuilder();

        sb.append(pad).append(stmt.identifier());

        if (!stmt.attributes().isEmpty()) {
            sb.append(" @[")
                    .append(stmt.attributes().stream()
                            .map(FileTest::formatAttribute)
                            .collect(Collectors.joining(", ")))
                    .append("]");
        }

        if (stmt.base() != null) {
            sb.append(" : ").append(stmt.base());
        }

        sb.append(" := ");
        sb.append(prettyPrintValue(stmt.value(), indent));

        return sb.toString();
    }

    private static String formatAttribute(Attribute attr) {
        if (attr.value() == null) return attr.key();
        return attr.key() + "=" + prettyPrintValue(attr.value(), 0);
    }

    private static String prettyPrintValue(Value v, int indent) {
        String pad   = "  ".repeat(indent);
        String inner = "  ".repeat(indent + 1);
        Source source = currentContext.source();

        String original = source.substring(v.start(), v.end());

        return switch (v) {
            case Value.StringValue s -> "\"" + escapeString(s.content()) + "\"";
            case Value.HexValue ignored when original.startsWith("#") -> original;
            case Value.BlobValue b -> b.toString();  // SACRED

            // ONE case for DoubleValue — prefer original if it has .0+, else format
            case Value.DoubleValue d -> {
                if (original.matches(".*\\.0+\\b")) {
                    yield original;  // preserve 64.0, 1.0
                }
                yield formatDouble(d.value());
            }

            case Value.NullValue      ignored -> "null";
            case Value.BooleanValue b        -> Boolean.toString(b.value());
            case Value.LongValue n           -> Long.toString(n.value());
            case Value.HexValue h            -> "0x" + Long.toHexString(h.value()).toUpperCase();
            case Value.BareLiteral b         -> b.value();

            case Value.ArrayValue a -> {
                if (a.elements().isEmpty()) yield "[]";
                String joined = a.elements().stream()
                        .map(e -> prettyPrintValue(e, indent + 1))
                        .collect(Collectors.joining(",\n" + inner));
                yield "[\n" + inner + joined + "\n" + pad + "]";
            }

            case Value.TupleValue t -> {
                String joined = t.elements().stream()
                        .map(e -> prettyPrintValue(e, indent + 1))
                        .collect(Collectors.joining(",\n" + inner));
                yield "(\n" + inner + joined + "\n" + pad + ")";
            }

            case Value.ObjectValue o -> {
                if (o.fields().isEmpty()) yield "{}";
                String joined = o.fields().stream()
                        .map(e -> e.getKey() + " := " + prettyPrintValue(e.getValue(), indent + 1))
                        .collect(Collectors.joining("\n" + inner));
                yield "{\n" + inner + joined + "\n" + pad + "}";
            }

            default -> throw new AssertionError("Unknown value type: " + v.getClass().getSimpleName());
        };
    }

    private static String formatDouble(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return Double.toString(d);
        String s = Double.toString(d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    // Enhanced result with exception capture
    record TestResult(
            String name,
            Path path,
            Context context,
            double parseTimeMs,
            boolean success,
            Exception exception
    ) {
        String formatTime() {
            if (parseTimeMs < 0.005) {
                return String.format("%.3f µs", parseTimeMs * 1000.0);
            }
            return String.format("%.6f ms", parseTimeMs);
        }
    }

    private static String formatParseTime(double parseTimeMs) {
        if (parseTimeMs < 0.01) {
            // Less than 10µs → show in µs with 1 decimal
            double us = parseTimeMs * 1000.0;
            return String.format("%.1fus", us);
        } else if (parseTimeMs < 1.0) {
            // 10µs to <1ms → show in µs with 1 decimal
            double us = parseTimeMs * 1000.0;
            return String.format("%.1fµs", us);
        } else {
            // 1ms and up → show in ms with 1 decimal
            return String.format("%.1fms", parseTimeMs);
        }
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}