// dev.badkraft.anvil.core.data.ValueBase.java
package dev.badkraft.anvil.core.data;

import java.util.Objects;

public final class ValueBase {
    final Source source;
    final int start;
    final int end;

    public ValueBase(Source source, int start, int end) {
        this.source = Objects.requireNonNull(source);
        this.start = start;
        this.end = end;
    }

    String source() {
        return source.substring(start, end);
    }

    public String substring() {
        return source.substring(start, end);
    }
}