// dev.badkraft.anvil.api/Anvil.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.core.data.Dialect;
import dev.badkraft.anvil.core.data.Statement;
import dev.badkraft.anvil.core.data.Value;
import dev.badkraft.anvil.utilities.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The entire public API of Anvil lives here.
 * <p>
 * There is only one runtime type you ever see: {@code Anvil}.
 * Everything else is internal.
 */
public class Anvil {

    // =================================================================== //
    // Factory methods — this is the only way users get an Anvil instance //
    // =================================================================== //

    public static AnvilRoot load(Path path, Dialect dialect, String namespace) throws IOException {
        return init(path, dialect, namespace);
    }
    public static AnvilRoot load(Path path, Dialect dialect) throws IOException {
        return load(path, dialect, Utils.createNamespaceFromPath(path));
    }
    public static AnvilRoot load(Path path) throws IOException {
        return load(path, Dialect.fromFileExtension(Utils.getFileExtension(path)), Utils.createNamespaceFromPath(path));
    }
    public static AnvilRoot read(String source, String namespace) {
        return init(source, Dialect.AML, namespace);
    }
    public static AnvilRoot read(String source) {
        return read(source, Utils.createNamespace());
    }
    private static AnvilRoot init(String source, Dialect dialect, String namespace) {
        Context ctx = Context.builder()
                .source(source)
                .dialect(dialect)
                .namespace(namespace)
                .build();
        ctx.parse();
        return buildRoot(ctx);
    }
    private static AnvilRoot init(Path path, Dialect dialect, String namespace) throws IOException {
        Context ctx = Context.builder()
                .source(path)
                .dialect(dialect)
                .namespace(namespace)
                .build();
        ctx.parse();
        return buildRoot(ctx);
    }
    private static AnvilRoot buildRoot(Context ctx) {
        return new AnvilRoot(
                ctx.attributes(),
                ctx.statements(),
                ctx.namespace()
        );
    }

}


