// src/test/java/dev/badkraft/anvil/parser/ModSpeedTest.java
package dev.badkraft.anvil.parser;

import dev.badkraft.anvil.api.Anvil;
import dev.badkraft.anvil.api.AnvilRoot;
import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.utilities.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModSpeedTest {
    private static final Path TEST_MODS_DIR = Paths.get("src/test/resources/mods");
    private static final Path LOG_FILE = Paths.get("logs/mod_metrics.log");
    private static final int WARMUP_CYCLES = 5_000;
    private static final int TEST_CYCLES = 100;

    public static void main(String[] args) throws IOException {
        ensureDirectories();
        List<Path> files = resolveModFiles();
        if (files.isEmpty()) {
            log("ERROR: No .aml files found in " + TEST_MODS_DIR);
            System.exit(1);
        }

        log("Starting Anvil v0.1.6 speed test with " + files.size() + " mod files...");
        warmup(files);
        List<TestResult> results = runTests(files);
        writeResults(results);
        summarize(results);

        log("Speed test complete. Metrics written to " + LOG_FILE);
    }

    private static void ensureDirectories() throws IOException {
        if (!Files.exists(TEST_MODS_DIR)) {
            log("ERROR: Test mods directory not found: " + TEST_MODS_DIR);
            System.exit(1);
        }
        Files.createDirectories(LOG_FILE.getParent());
    }

    private static List<Path> resolveModFiles() throws IOException {
        try (var stream = Files.list(TEST_MODS_DIR)) {
            return stream
                    .filter(p -> p.toString().endsWith(".aml"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static void warmup(List<Path> files) throws IOException {
        log("Warming up JVM with " + WARMUP_CYCLES + " cycles...");
        long start = System.nanoTime();
        for (int i = 0; i < WARMUP_CYCLES; i++) {
            for (Path file : files.subList(0, Math.min(10, files.size()))) { // first 10 files
                try {
                    Anvil.load(file);
                } catch (Exception ignored) {
                    // We just want the JIT hot
                }
            }
        }
        System.gc(); // Force cleanup
        double warmupMs = (System.nanoTime() - start) / 1_000_000.0;
        log(String.format("Warmup completed in %.1f ms", warmupMs));
    }

    private static List<TestResult> runTests(List<Path> files) throws IOException {
        List<TestResult> results = new ArrayList<>();
        for (Path file : files) {
            long fileSizeBytes = Files.size(file);
            long startMemory = getUsedMemory();
            long startTime = System.nanoTime();

            AnvilRoot root = null;
            Exception error = null;
            for (int i = 0; i < TEST_CYCLES; i++) {
                try {
                    root = Anvil.load(file);
                } catch (Exception e) {
                    error = e;
                    break;
                }
            }

            double parseTimeMs = (System.nanoTime() - startTime) / (1_000_000.0 * TEST_CYCLES);
            long memoryUsedBytes = getUsedMemory() - startMemory;

            int statementCount = root != null ? root.statements().size() : 0;
            int attributeCount = root != null ? root.rootAttributes().size() : 0;

            results.add(new TestResult(
                    file.getFileName().toString(),
                    fileSizeBytes,
                    parseTimeMs,
                    memoryUsedBytes,
                    statementCount,
                    attributeCount,
                    error == null,
                    error
            ));
        }
        return results;
    }

    private static long getUsedMemory() {
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void writeResults(List<TestResult> results) throws IOException {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("Timestamp: ").append(Instant.now()).append("\n");
        logEntry.append("Files: ").append(results.size()).append("\n");
        logEntry.append("Test Cycles per File: ").append(TEST_CYCLES).append("\n");
        logEntry.append("Metrics:\n");
        logEntry.append("File,Size (B),Time (ms),Memory (B),Statements,Attributes,Success,Error\n");

        for (TestResult r : results) {
            logEntry.append(String.format(
                    "%s,%d,%.3f,%d,%d,%d,%s,%s\n",
                    r.name,
                    r.fileSizeBytes,
                    r.parseTimeMs,
                    r.memoryUsedBytes,
                    r.statementCount,
                    r.attributeCount,
                    r.success,
                    r.error != null ? r.error.getMessage() : ""
            ));
        }

        Files.writeString(
                LOG_FILE,
                logEntry.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static void summarize(List<TestResult> results) {
        double avgTimeMs = results.stream().mapToDouble(r -> r.parseTimeMs).average().orElse(0);
        long totalSizeBytes = results.stream().mapToLong(r -> r.fileSizeBytes).sum();
        long totalMemoryBytes = results.stream().mapToLong(r -> r.memoryUsedBytes).sum();
        int totalStatements = results.stream().mapToInt(r -> r.statementCount).sum();
        int totalAttributes = results.stream().mapToInt(r -> r.attributeCount).sum();
        long failed = results.stream().filter(r -> !r.success).count();

        log("Summary:");
        log(String.format("  Files: %d", results.size()));
        log(String.format("  Total Size: %.2f MB", totalSizeBytes / (1024.0 * 1024.0)));
        log(String.format("  Avg Parse Time: %.3f ms", avgTimeMs));
        log(String.format("  Total Memory: %.2f MB", totalMemoryBytes / (1024.0 * 1024.0)));
        log(String.format("  Total Statements: %d", totalStatements));
        log(String.format("  Total Attributes: %d", totalAttributes));
        log(String.format("  Success Rate: %.2f%%", 100.0 * (results.size() - failed) / results.size()));
        if (failed > 0) {
            log(String.format("  Failures: %d", failed));
        }
    }

    private static void log(String msg) {
        System.out.println("[ModSpeedTest] " + msg);
    }

    record TestResult(
            String name,
            long fileSizeBytes,
            double parseTimeMs,
            long memoryUsedBytes,
            int statementCount,
            int attributeCount,
            boolean success,
            Exception error
    ) {}
}