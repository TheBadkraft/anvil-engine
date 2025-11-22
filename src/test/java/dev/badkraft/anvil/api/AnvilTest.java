package dev.badkraft.anvil.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Black-box contract test for the public Anvil API.
 * <p>
 * Uses only the public façade (Anvil.parse(...), AnvilModule, AnvilValue).
 * No knowledge of parser internals.
 * </p>
 */
public class AnvilTest {

    private static final Path TEST_RESOURCES = Paths.get("build/resources/test");
    private static final boolean VERBOSE = !Boolean.getBoolean("quiet");

    public static void main(String[] args) throws IOException {
        if (!Files.isDirectory(TEST_RESOURCES)) {
            System.err.println("Test resources not found at: " + TEST_RESOURCES);
            System.err.println("Run ./gradlew processTestResources first.");
            System.exit(1);
        }

        List<Path> files = resolveFiles(args);

        if (files.isEmpty()) {
            System.out.println("No .aml files found to test.");
            return;
        }

        List<Result> results = new ArrayList<>();
        for (Path file : files) {
            results.add(testFile(file));
        }

        if(VERBOSE) {
//            for (int i = 0; i < 100; i++) {
//                long start = System.nanoTime();
//                Anvil.parse(files.get(0));
//                System.out.println("Run x" + i + "::" + (System.nanoTime() - start)/1_000_000.0 + " ms");
//            }
            long sum = 0;
            int runs = 1000;
            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                Anvil.parse(files.get(0));
                sum += System.nanoTime() - start;
            }
            System.out.println("Average over " + runs + ": " + (sum / runs / 1_000_000.0) + " ms");
        }

        long failed = results.stream().filter(r -> !r.success).count();
        System.out.printf("%n=== ANVIL PUBLIC API TEST SUMMARY ===%n");
        System.out.printf("Total: %d   Passed: %d   Failed: %d%n",
                results.size(), results.size() - failed, failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static List<Path> resolveFiles(String[] args) throws IOException {
        if (args.length > 0) {
            Path single = Paths.get(args[0]);
            return Files.isRegularFile(single) ? List.of(single) : List.of();
        }

        return Files.list(TEST_RESOURCES)
                .filter(p -> p.toString().endsWith(".aml"))
                .sorted()
                .toList();
    }

    private static Result testFile(Path path) {
        String name = path.getFileName().toString();
        System.out.printf("%n--- %s ---%n", name);

        long start = System.nanoTime();
        List<String> errors = new ArrayList<>();
        boolean success = true;
        AnvilModule module = null;

        try {
            module = Anvil.parse(path);
        } catch (Exception e) {
            success = false;
            errors.add("Parse failed: " + e);
            return new Result(name, elapsed(start), errors, success);
        }

        try {
            Set<String> keys = module.keys();

            if (keys.isEmpty()) {
                errors.add("Module reports zero top-level keys");
                success = false;
            }

//            // Fast-fail path – every key from keys() must be get()-able
//            for (String k : keys) {
//                module.get(k); // should never throw NoSuchKeyException
//            }

//            // Safe path – non-existent key must return empty optional
//            if (module.tryGet("___THIS_KEY_DOES_NOT_EXIST___").isPresent()) {
//                errors.add("tryGet returned present for non-existent key");
//                success = false;
//            }

            // Smoke-test at least one convenience method
            if (!keys.isEmpty()) {
                module.getString(keys.iterator().next());
            }

            if (VERBOSE) {
                System.out.printf("  namespace : %s%n", module.namespace());
                System.out.printf("  source    : %s%n", module.source());
                System.out.printf("  keys (%d) : %s%n", keys.size(), keys);
            }

        } catch (Exception e) {
            success = false;
            errors.add("Contract violation: " + e);
        }

        double ms = elapsed(start);
        if (success && VERBOSE) {
            System.out.printf("  PASS – %.3f ms%n", ms);
        }

        return new Result(name, ms, errors, success);
    }

    private static double elapsed(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000.0;
    }

    private record Result(String file, double timeMs, List<String> errors, boolean success) {
        private Result {
            errors = List.copyOf(errors);
        }
    }
}