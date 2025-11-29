// src/main/java/dev/badkraft/engine/writer/ModuleWriter.java
package dev.badkraft.anvil.writer;

import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.core.data.*;

import java.util.List;
import java.util.stream.Collectors;

public final class ModuleWriter {
    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;
    private final boolean isPretty;

    public ModuleWriter(boolean pretty) {
        this.isPretty = pretty;
    }

    public String write(Context context) {
        if (!context.isValid()) {
            throw new IllegalStateException("Cannot write invalid module");
        }

        if (context.dialect() != Dialect.NONE) {
            sb.append("#!").append(context.dialect().name().toLowerCase()).append('\n');
            if (isPretty) sb.append('\n');
        }

        List<Statement> stmts = context.statements();
        for (int i = 0; i < stmts.size(); i++) {
            if (stmts.get(i) instanceof Assignment a) {
                writeAssignment(a);
            }
            if (i < stmts.size() - 1) {
                sb.append(isPretty ? "\n" : ",");
            }
        }
        return sb.toString();
    }

    private void writeAssignment(Assignment a) {
        if (isPretty) indent();
        sb.append(a.key());

        if (!a.attributes().isEmpty()) {
            sb.append(" @[")
                    .append(a.attributes().stream()
                            .map(Attribute::toString)
                            .collect(Collectors.joining(", ")))
                    .append(']');
        }

        sb.append(isPretty ? " := " : ":=");
        writeValue(a.value());
    }

    private void writeValue(Value v) {
        if (v instanceof Value.ObjectValue ov) {
            // Open the object
            sb.append(isPretty ? " {" : " {");

            if (isPretty) {
                indent++;
            }

            boolean first = true;
            for (var e : ov.fields()) {
                if (!first) {
                    sb.append(',');
                }
                if (isPretty) {
                    sb.append('\n');
                    indent();
                }
                sb.append(e.getKey()).append(" := ");
                writeValue(e.getValue());
                first = false;
            }

            // Close the object
            if (isPretty && !ov.fields().isEmpty()) {
                indent();
            }
            sb.append("\n}");
            if (isPretty) {
                indent--;
            }
        }
        else if (v instanceof Value.ArrayValue av) {
            sb.append('[');
            if (isPretty && !av.elements().isEmpty()) {
                sb.append('\n');
                indent++;
                indent();
            }

            for (int i = 0; i < av.elements().size(); i++) {
                if (i > 0) {
                    sb.append(',');
                    if (isPretty) sb.append('\n').append("  ".repeat(indent));
                }
                writeValue(av.elements().get(i));
            }

            if (isPretty && !av.elements().isEmpty()) {
                sb.append('\n');
                indent--;
                indent();
            }
            sb.append(']');
        }
        else {
            sb.append(v.toString());
        }
    }

    private void indent() {
        if (isPretty) {
            sb.append("  ".repeat(indent));
        }
    }
}