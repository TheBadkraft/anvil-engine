// src/test/java/dev/badkraft/aurora/parser/FileTest.java
package dev.badkraft.anvil.parser;

import dev.badkraft.anvil.*;
import dev.badkraft.anvil.Module;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class FileTest {
    private static final Path TEST_DIR = Paths.get("build/resources/test");

    public static void main(String[] args) {
        ensureTestDirectory();

        List<Path> files;
        if (args.length > 0) {
            Path file = TEST_DIR.resolve(args[0]);
            if (!Files.exists(file)) {
                System.err.println("File not found: " + file);
                return;
            }
            files = List.of(file);
        } else {
            try (var stream = Files.list(TEST_DIR)) {
                files = stream
                        .filter(p -> p.toString().endsWith(".aml"))
                        .sorted()
                        .toList();
            } catch (IOException e) {
                System.err.println("Failed to read test directory: " + TEST_DIR);
                e.printStackTrace();
                return;
            }

            if (files.isEmpty()) {
                System.out.println("No .aml files found in " + TEST_DIR);
                return;
            }

            System.out.println("Running all " + files.size() + " test files in " + TEST_DIR + "\n");
        }

        List<TestResult> results = new ArrayList<>();
        for (Path path : files) {
            TestResult result = runSingleFile(path);
            results.add(result);
            printResult(result);
            System.out.println(); // spacing
        }

        // Summary
        long failed = results.stream().filter(r -> !r.success).count();
        System.out.printf("SUMMARY: %d/%d passed%n", results.size() - failed, results.size());
        if (failed > 0) {
            System.out.println("Some tests failed. Fix them. Now.");
            System.exit(1);
        }
    }

    private static void ensureTestDirectory() {
        if (!Files.exists(TEST_DIR)) {
            System.err.println("Test directory not found: " + TEST_DIR);
            System.err.println("Run ./gradlew processTestResources or build first.");
            System.exit(1);
        }
    }

    private static TestResult runSingleFile(Path path) {
        String fileName = path.getFileName().toString();
        System.out.println("=== " + fileName + " ===");

        long start = System.nanoTime();
        ParseResult<?> result = AnvilParser.parse(path);
        long parseTime = (System.nanoTime() - start) / 1_000_000;

        boolean success = result.isSuccess() && result.errors().isEmpty();
        var module = success ? (dev.badkraft.anvil.Module) result.result() : null;

        return new TestResult(fileName, path, module, result.errors(), parseTime, success);
    }

    private static void printResult(TestResult r) {
        if (!r.success) {
            System.out.printf("FAILED — %d error(s) in %.3f ms%n", r.errors.size(), r.parseTime);
            r.errors.forEach(e ->
                    System.err.printf(" [%d:%d] %s%n", e.line(), e.col(), e.message())
            );
            return;
        }

        dev.badkraft.anvil.Module module = r.module;
        System.out.printf("PASS — %.3f ms%n", r.parseTime);
        System.out.println("Module [" + module.namespace() + "] \n   dialect: " + module.dialect());
        System.out.println();

        if (!module.statements().isEmpty()) {
            System.out.println("Statements:");
            for (Statement stmt : module.statements()) {
                System.out.println(prettyPrintStatement(stmt, 1));
            }
        } else {
            System.out.println("(no statements)");
        }
    }

    // === Pretty printer (unchanged, just moved) ===
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

        sb.append(" := ");

        if (stmt.value() instanceof Value.ObjectValue obj && obj.source() != null) {
            sb.append(obj.source());
        } else if (stmt.value() instanceof Value.ArrayValue arr && arr.source() != null) {
            sb.append(arr.source());
        } else {
            sb.append(prettyPrintValue(stmt.value(), indent));
        }

        return sb.toString();
    }

    private static String formatAttribute(Attribute attr) {
        if (attr.value() == null) return attr.key();
        return attr.key() + "=" + prettyPrintValue(attr.value(), 0);
    }

    private static String prettyPrintValue(Value value, int indent) {
        String pad = "  ".repeat(indent);
        String inner = "  ".repeat(indent + 1);

        return switch (value) {
            case Value.NullValue ignored -> "null";
            case Value.BooleanValue b -> Boolean.toString(b.value());
            case Value.HexValue n  -> n.source() != null ? n.source() : Long.toString(n.value());
            case Value.LongValue n -> n.source() != null ? n.source() : Long.toString(n.value());
            case Value.DoubleValue d -> d.source() != null ? d.source() : Double.toString(d.value());
            case Value.StringValue s  -> "\"" + s.value()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t") + "\"";
            case Value.BareLiteral b  -> b.id();
            case Value.BlobValue f -> (f.attribute() != null ? "@" + f.attribute() : "@") + f.content();

            case Value.ArrayValue a -> {
                if (a.source() != null) yield a.source();
                if (a.elements().isEmpty()) yield "[]";
                String joined = a.elements().stream()
                        .map(e -> prettyPrintValue(e, indent + 1))
                        .collect(Collectors.joining(",\n" + inner));
                yield "[\n" + inner + joined + "\n" + pad + "]";
            }

            case Value.ObjectValue o -> {
                if (o.source() != null) yield o.source();
                if (o.fields().isEmpty()) yield "{}";
                String joined = o.fields().stream()
                        .map(e -> e.getKey() + " := " + prettyPrintValue(e.getValue(), indent + 1))
                        .collect(Collectors.joining(",\n" + inner));
                yield "{\n" + inner + joined + "\n" + pad + "}";
            }

            case Value.TupleValue t -> {
                if (t.source() != null) yield t.source();
                String joined = t.elements().stream()
                        .map(e -> prettyPrintValue(e, indent + 1))
                        .collect(Collectors.joining(", "));
                yield "(\n" + inner + joined + "\n" + pad + ")";
            }

            default -> "<unknown:" + value.getClass().getSimpleName() + ">";
        };
    }

    // Simple result holder
    record TestResult(
            String name,
            Path path,
            Module module,
            List<ParseError> errors,
            double parseTime,
            boolean success
    ) {}
}