package dev.badkraft.anvil.api;

import dev.badkraft.anvil.Attribute;
import dev.badkraft.anvil.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AnvilValueAdapter {

    private AnvilValueAdapter() {}

    static AnvilValue adapt(Value value) {
        if (value == null) {
            return new AnvilNull();
        }
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
                            .toList(),
                    arr.attributes().stream()
                            .map(attr -> new AnvilAttribute(
                                    Map.entry(new AnvilBare(attr.key()), adapt(attr.value()))
                            ))
                            .toList()
            );

            case Value.TupleValue tup -> new AnvilTuple(
                    tup.elements().stream()
                            .map(AnvilValueAdapter::adapt)
                            .toList(),
                    tup.attributes().stream()
                            .map(attr -> new AnvilAttribute(
                                    Map.entry(new AnvilBare(attr.key()), adapt(attr.value()))
                            ))
                            .toList()
            );

            case Value.ObjectValue obj -> {
                Map<String, AnvilValue> fields = obj.fields().stream()
                        .collect(Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                e -> adapt(e.getValue())
                        ));
                List<AnvilAttribute> attributes = obj.attributes().stream()
                        .map(attr -> new AnvilAttribute(
                                Map.entry(new AnvilBare(attr.key()), adapt(attr.value()))
                        ))
                        .toList();
                AnvilModule nested = new ImmutableAnvilModule(fields, attributes);
                yield new AnvilObject(nested);
            }

            default -> throw new IllegalStateException("Unknown Value type: " + value.getClass());
        };
    }
    static List<AnvilAttribute> adapt(List<Attribute> attributes) {
        return attributes.stream()
                .map(attr -> new AnvilAttribute(
                        Map.entry(new AnvilBare(attr.key()), adapt(attr.value()))
                ))
                .toList();
    }
}