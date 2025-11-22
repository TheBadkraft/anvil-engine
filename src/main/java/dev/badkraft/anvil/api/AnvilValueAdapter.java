package dev.badkraft.anvil.api;

import dev.badkraft.anvil.Value;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AnvilValueAdapter {

    private AnvilValueAdapter() {}

    static AnvilValue adapt(Value value) {
        return switch (value) {
            case Value.NullValue ignored -> new AnvilNull();
            case Value.BooleanValue b -> new AnvilBoolean(b.value());
            case Value.LongValue l -> new AnvilNumeric(l.value());
            case Value.DoubleValue d -> new AnvilNumeric(d.value());
            case Value.HexValue h -> new AnvilNumeric(h.value());
            case Value.StringValue s -> new AnvilString(s.value());
            case Value.BareLiteral b -> new AnvilBare(b.id());
            case Value.BlobValue blob -> new AnvilBlob(blob.content(), blob.attribute());

            case Value.ArrayValue arr -> new AnvilArray(
                    arr.elements().stream()
                            .map(AnvilValueAdapter::adapt)
                            .toList()
            );

            case Value.TupleValue tup -> new AnvilTuple(
                    tup.elements().stream()
                            .map(AnvilValueAdapter::adapt)
                            .toList()
            );

            case Value.ObjectValue obj -> {
                Map<String, AnvilValue> fields = obj.fields().stream()
                        .collect(Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                e -> adapt(e.getValue())
                        ));

                AnvilModule nested = new ImmutableAnvilModule(fields);
                yield new AnvilObject(nested);
            }

            default -> throw new IllegalStateException("Unknown Value type: " + value.getClass());
        };
    }
}