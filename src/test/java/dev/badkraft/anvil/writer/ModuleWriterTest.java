// src/test/java/aurora/engine/writer/ModuleWriterTest.java
package dev.badkraft.anvil.writer;

import dev.badkraft.anvil.parser.AnvilParser;
import dev.badkraft.anvil.Module;
import dev.badkraft.anvil.ParseResult;
import dev.badkraft.anvil.writer.ModuleWriter;

import java.nio.file.*;
import java.io.IOException;
import java.util.stream.Collectors;

public class ModuleWriterTest {
    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : "build/resources/test/attribs.aml";
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            System.err.println("File not found: " + path);
            return;
        }

        System.out.println("=== ModuleWriter Round-Trip Test ===");
        System.out.println("Input: " + path);

        ParseResult<Module> result = AnvilParser.parse(path);

        if (!result.isSuccess()) {
            System.err.println("Parse failed:");
            result.errors().forEach(e -> System.err.printf("  [%d:%d] %s%n", e.line(), e.col(), e.message()));
            return;
        }

        Module module = result.result();
        if (!module.isValid()) {
            System.err.println("Module is invalid (duplicate keys/ids)");
            return;
        }

        String written = new ModuleWriter(true).write(module);
        System.out.println("\n--- Pretty Print Output ---");
        System.out.println(written);
        System.out.println("\n--- Normalized Comparison ---");

        String original;
        try {
            original = Files.readString(path).replace("\r\n", "\n");
        } catch (IOException e) {
            System.err.println("Failed to read original file: " + e);
            return;
        }

        boolean match = minify(original).equals(minify(written));
        System.out.println("Round-trip: " + (match ? "PASS" : "FAIL"));

        if (!match) {
            System.out.println("\n--- Expected (minified) ---");
            System.out.println(minify(original));
            System.out.println("\n--- Got (minified) ---");
            System.out.println(minify(written));
        } else {
            System.out.println("Minified semantic matches input exactly.");
        }
    }

    /** Normalize for semantic comparison: remove comments, trim lines, preserve blank lines between statements */
    private static String normalize(String input) {
        return input
                .lines()
                .map(line -> {
                    int commentIdx = line.indexOf("//");
                    return commentIdx >= 0 ? line.substring(0, commentIdx).trim() : line.trim();
                })
                .collect(Collectors.joining("\n"))
                .replaceAll("\n{3,}", "\n\n") // collapse 3+ blank lines → 2
                .replaceAll("^\n+", "\n")     // remove leading blank lines
                .replaceAll("\n+$", "\n");    // ensure single trailing newline
    }

    // prototype: minify for compact semantic comparison
    private static String minify(String input) {
        // Step 1: Strip ALL commas and comments
        String noComments = input
                .replaceAll("//.*", "")
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll(",", "");  // <--- STRIP ALL COMMAS

        // Step 2: Split into lines
        String[] lines = noComments.split("\r?\n");

        // Step 3: Rebuild with smart comma logic
        StringBuilder sb = new StringBuilder();
        boolean needsComma = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Skip structural tokens
            if (trimmed.equals("{") || trimmed.equals("}") ||
                    trimmed.equals("[") || trimmed.equals("]")) {
                sb.append(trimmed);
                needsComma = !trimmed.endsWith("{") && !trimmed.endsWith("[");
                continue;
            }

            // Add comma if needed
            if (needsComma) {
                sb.append(",");
            }

            sb.append(trimmed);

            // Next line needs comma unless this ends with { or [
            needsComma = !trimmed.endsWith("{") && !trimmed.endsWith("[");
        }

        String result = sb.toString();

        // Step 4: Clean up
        result = result.replaceAll("^#!aml\\s+", "#!aml ");
        result = result.replaceAll("\\s*\\{\\s*", "{");
        result = result.replaceAll("\\s*\\}\\s*", "}");
        result = result.replaceAll("\\s*\\[\\s*", "[");
        result = result.replaceAll("\\s*\\]\\s*", "]");
        result = result.replaceAll("\\s*:=\\s*", ":=");

        return result;
    }
}