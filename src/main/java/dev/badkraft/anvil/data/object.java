/// src/main/java/dev/badkraft/anvil/data/null.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: 12 01, 2025
///
/// MIT License
/// Permission is hereby granted, free of charge, to any person obtaining a copy
/// of this software and associated documentation files (the "Software"), to deal
/// in the Software without restriction, including without limitation the rights
/// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
/// copies of the Software, and to permit persons to whom the Software is
/// furnished to do so, subject to the following conditions:
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
/// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
/// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
/// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
/// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
/// SOFTWARE.
package dev.badkraft.anvil.data;

import dev.badkraft.anvil.api.node;
import dev.badkraft.anvil.core.data.Attribute;
import dev.badkraft.anvil.core.data.Value;
import dev.badkraft.anvil.utilities.AnvilConverters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class object implements value {
    private final String base;
    private final LinkedHashMap<String, attribute> attributes;
    private final LinkedHashMap<String, value> fields;

    public object(Value.ObjectValue internal) {
        this.attributes = internal.attributes().stream()
                .collect(Collectors.toMap(
                        Attribute::key,
                        AnvilConverters::toAttribute,
                        (a, b) -> { throw new IllegalStateException("Duplicate key"); },
                        LinkedHashMap::new
                ));
        this.fields = internal.fields().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> AnvilConverters.toValue(e.getValue()),
                        (a, b) -> { throw new IllegalStateException("Duplicate key"); },
                        LinkedHashMap::new
                ));
        this.base = internal.base() == null ? "" : internal.base();
    }

    public value get(String field) {
        return fields.get(field);
    }
    public boolean has(String field) {
        return fields.containsKey(field);
    }
    public Set<String> fields() {
        return fields.keySet();
    }
    public List<attribute> attributes() {
        return List.copyOf(attributes.values());
    }
    @Override
    public object asObject() {
        return this;
    }
    @Override
    public boolean hasBase() {
        return base != null && !base.isEmpty();
    }
    @Override
    public String base() {
        return base;
    }
    public attribute attribute(String key) {
        if (!attributes.containsKey(key)) {
            throw new IllegalArgumentException("No attribute found with key: " + key);
        }
        return attributes.get(key);
    }
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    @Override
    public boolean isObject() { return true; }

    // ── Typed throwing getters ─────────────────────────────────────────────
    public String getString(String field) {
        return requireField(field).asString();
    }
    public long getLong(String field) {
        return requireField(field).asLong();
    }
    public int getInt(String field) {
        return requireField(field).asInt();
    }
    public double getDouble(String field) {
        return requireField(field).asDouble();
    }
    public boolean getBoolean(String field) {
        return requireField(field).asBoolean();
    }
    public object getObject(String field) {
        return requireField(field).asObject();
    }
    public array getArray(String field) {
        return requireField(field).asArray();
    }

    // ── Default-value overloads — never throw ─────────────────────────────
    public String getString(String field, String def) {
        value v = fields.get(field);
        if (v == null || v instanceof value.NullValue) return def;
        try { return v.asString(); } catch (ClassCastException e) { return def; }
    }
    public long getLong(String field, long def) {
        value v = fields.get(field);
        if (v == null || v instanceof value.NullValue) return def;
        try { return v.asLong(); } catch (ClassCastException e) { return def; }
    }
    public int getInt(String field, int def) {
        value v = fields.get(field);
        if (v == null || v instanceof value.NullValue) return def;
        try { return v.asInt(); } catch (ClassCastException | ArithmeticException e) { return def; }
    }
    public double getDouble(String field, double def) {
        value v = fields.get(field);
        if (v == null || v instanceof value.NullValue) return def;
        try { return v.asDouble(); } catch (ClassCastException e) { return def; }
    }
    public boolean getBoolean(String field, boolean def) {
        value v = fields.get(field);
        if (v == null || v instanceof value.NullValue) return def;
        try { return v.asBoolean(); } catch (ClassCastException e) { return def; }
    }

    private value requireField(String field) {
        value v = fields.get(field);
        if (v == null) throw new java.util.NoSuchElementException("No field: " + field);
        return v;
    }
}
