package dev.badkraft.anvil.core.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface Value
        permits Value.ArrayValue, Value.BareLiteral, Value.BlobValue, Value.BooleanValue,
        Value.DoubleValue, Value.HexValue, Value.LongValue, Value.NullValue,
        Value.ObjectValue, Value.StringValue, Value.TupleValue {

    default Attributes getAttributes() {
        return switch (this) {
            case ArrayValue a -> new Attributes(a.attributes);
            case TupleValue t -> new Attributes(t.attributes);
            case ObjectValue o -> new Attributes(o.attributes);
            default -> throw new UnsupportedOperationException(
                    "Attributes not supported on " + getClass().getSimpleName());
        };
    }

    int start();
    int end();

    final class Attributes {
        private final List<Attribute> backing;
        private Attributes(List<Attribute> backing) {
            this.backing = Objects.requireNonNullElse(backing, new ArrayList<>());
        }
        public void add(Attribute attribute) { backing.add(attribute); }
        public void addAll(List<Attribute> attributes) { backing.addAll(attributes); }
        public boolean isEmpty() { return backing.isEmpty(); }
        public int size() { return backing.size(); }
        public Attribute get(int index) { return backing.get(index); }
        public boolean has(String key) { return backing.stream().anyMatch(a -> a.key().equals(key)); }
        public boolean hasTag(String key) { return backing.stream().anyMatch(a -> a.key().equals(key) && a.value() == null); }
        public Optional<Attribute> find(String key) { return backing.stream().filter(a -> a.key().equals(key)).findFirst(); }
        public Stream<Attribute> stream() { return backing.stream(); }
        public Iterator<Attribute> iterator() { return backing.iterator(); }
    }

    // === PRIMITIVES ===
    record NullValue(ValueBase base) implements Value {
        public NullValue(Source source, int start, int end) {
            this(new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return "null"; }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record BooleanValue(boolean value, ValueBase base) implements Value {
        public BooleanValue(Source source, boolean value, int start, int end) {
            this(value, new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record LongValue(long value, ValueBase base) implements Value {
        public LongValue(Source source, long value, int start, int end) {
            this(value, new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record DoubleValue(double value, ValueBase base) implements Value {
        public DoubleValue(Source source, double value, int start, int end) {
            this(value, new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record HexValue(long value, ValueBase base) implements Value {
        public HexValue(Source source, long value, int start, int end) {
            this(value, new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record StringValue(ValueBase base) implements Value {
        public StringValue(Source source, int start, int end) {
            this(new ValueBase(source, start, end));
        }
        public String content() { return base.substring();}
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record BareLiteral(ValueBase base) implements Value {
        public BareLiteral(Source source, int start, int end) {
            this(new ValueBase(source, start, end));
        }
        public String value() { return base.substring();}
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record BlobValue(String attribute, ValueBase base) implements Value {
        public BlobValue(Source source, String attribute, int start, int end) {
            this(attribute, new ValueBase(source, start, end));
        }
        public String attribute() { return attribute; }
        public String value() { return base.substring();}
        @Override public @NotNull String toString() { return (attribute != null ? "@" + attribute : "@") + value(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    // === COMPOSITES ===
    record ArrayValue(List<Value> elements, List<Attribute> attributes, ValueBase base) implements Value {
        public ArrayValue(Source source, List<Value> elements, List<Attribute> attributes, int start, int end) {
            this(List.copyOf(elements), new ArrayList<>(attributes != null ? attributes : List.of()), new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record TupleValue(List<Value> elements, List<Attribute> attributes, ValueBase base) implements Value {
        public TupleValue(Source source, List<Value> elements, List<Attribute> attributes, int start, int end) {
            this(List.copyOf(elements), new ArrayList<>(attributes != null ? attributes : List.of()), new ValueBase(source, start, end));
            if (elements.size() < 2) throw new IllegalArgumentException("Tuple must have at least 2 elements");
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }
    }

    record ObjectValue(List<Map.Entry<String, Value>> fields, List<Attribute> attributes, ValueBase base) implements Value {
        public ObjectValue(Source source, List<Map.Entry<String, Value>> fields, List<Attribute> attributes, int start, int end) {
            this(List.copyOf(fields), new ArrayList<>(attributes != null ? attributes : List.of()), new ValueBase(source, start, end));
        }
        @Override public @NotNull String toString() { return base.source(); }
        @Override public int start() { return base.start; }
        @Override public int end() { return base.end; }

        public Map<String, Value> asMap() {
            LinkedHashMap<String, Value> map = new LinkedHashMap<>();
            for (var e : fields) {
                if (map.put(e.getKey(), e.getValue()) != null) {
                    throw new IllegalStateException("Duplicate key: " + e.getKey());
                }
            }
            return Map.copyOf(map);
        }
    }
}