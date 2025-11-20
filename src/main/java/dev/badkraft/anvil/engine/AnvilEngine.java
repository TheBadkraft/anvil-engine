// src/main/java/dev/badkraft/anvil/engine/AuroraEngine.java
package dev.badkraft.anvil.engine;

import dev.badkraft.anvil.parser.AnvilParser;
import dev.badkraft.anvil.Module;
import dev.badkraft.anvil.ParseResult;

import java.nio.file.*;

public final class AnvilEngine {
    private static final Path DATA_ROOT = Path.of("data/aurora");

    public static void main(String[] args) {
        System.out.println("Aurora Engine starting...");
        if (!Files.exists(DATA_ROOT)) {
            System.out.println("No data/aurora/ directory found. Create it and add .aml files.");
            return;
        }

        Registry registry = new Registry();
        loadAmlFiles(registry, DATA_ROOT);

        System.out.println("\n=== Registry Summary ===");
//        registry.printSummary();
    }

    private static void loadAmlFiles(Registry registry, Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(".aml"))
                    .forEach(path -> {
                        System.out.println("Loading: " + path);
                        ParseResult<Module> result = AnvilParser.parse(path);
                        if (!result.errors().isEmpty()) {
                            System.out.println("  Failed:");
                            result.errors().forEach(
                                    e -> System.err.printf("    [%d:%d] %s%n", e.line(), e.col(), e.message()));
                        } else {
//                            result.result().forEach(registry::register);
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error scanning data/aurora: " + e);
        }
    }
}

class Registry {
//    private final Map<String, NamedModel> models = new HashMap<>();
//
//    void register(NamedModel model) {
//        models.put(model.fullId(), model);
//    }
//
//    void printSummary() {
//        if (models.isEmpty()) {
//            System.out.println("No models registered.");
//            return;
//        }
//        models.forEach((id, model) -> System.out.printf("• %s [%s] — %d fields, %d children%n",
//                id, model.attributes().getOrDefault("type", new Model.Attribute("type", "unknown")).value(),
//                model.fields().size(), model.children().size()));
//    }
}