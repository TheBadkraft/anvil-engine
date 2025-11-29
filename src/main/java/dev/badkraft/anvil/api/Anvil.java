package dev.badkraft.anvil.api;

import dev.badkraft.anvil.core.data.Dialect;
import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.utilities.Utils;

import java.io.IOException;
import java.nio.file.Path;

public final class Anvil {
    private Anvil() {}

    /*
        Create a new Anvil context.
        - Load: from file path
            - optional: Dialect
                - if not provided, use the file's extension
                - or, default to AsL
            - optional: namespace
                - if not provided, use "default01"
        - Read: from String
            - optional: Dialect
                - if not provided, default to ASL
            - optional: namespace
                - if not provided, use "default01"
     */
    public static Context load(Path path, Dialect dialect, String namespace) throws IOException {
        // Implementation would go here
        return init(Utils.loadFile(path), dialect, namespace);
    }
    public static Context load(Path path) throws IOException {
        return load(path, Dialect.ASL, Utils.createNamespaceFromPath(path));
    }
    public static Context read(String source, String namespace) {
        // Implementation would go here
        return init(source, Dialect.ASL, Utils.createNamespace());
    }
    public static Context read(String source) {
        return read(source, "default01");
    }

    //  all functions feed into this one
    private static Context init(String source, Dialect dialect, String namespace) {
        // Implementation would go here
        /*
            Pseudocode:
            1. Create a new Context using builder
                a. determine dialect
                b. use hint if provided, otherwise default
            2. Init the Source object with the source string.
            3. Context.parse() -- calls parser.pars(context)
            4. Return the populated Context.
         */
        return null;
    }

    // Future public surface lands here or in sibling types
    // public static AnvilModule merge(AnvilModule... modules) { ... }
    // public static void hotReload(Path changed) { ... }
}