/// src/main/java/dev/badkraft/anvil/api/node.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: December 1, 2025
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
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.data.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public final class node {
    private final String identifier;
    private final LinkedHashMap<String, attribute> attributes;
    private final value value;

    public node(String identifier, List<attribute> attributes, value value) {
        this.identifier = identifier;
        this.attributes = attributes.stream()
                .collect(Collectors.toMap(
                        attribute::key,
                        a -> a,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        this.value = value;
    }

    public String identifier() {
        return identifier;
    }
    public List<attribute> attributes() {
        return List.copyOf(attributes.values());
    }
    public value value() {
        return value;
    }
    public boolean hasAttribute(String key) {
        return switch (value) {
            case object obj -> obj.hasAttribute(key);
            case array arr  -> arr.hasAttribute(key);
            case tuple tup  -> tup.hasAttribute(key);
            case blob b     -> b.hasTag();
            case null, default -> throw new NoSuchElementException("No attribute: @" + key);
        };
    }
    public value attribute(String key) {
        return switch (value) {
            case object obj -> obj.attribute(key).value();
            case array arr  -> arr.attribute(key).value();
            case tuple tup  -> tup.attribute(key).value();
            case blob b     -> b.tag();
            case null, default -> throw new NoSuchElementException("No attribute: @" + key);
        };
    }
    public value get(String field) {
        return switch (value) {
            case object obj -> obj.get(field);
            case null, default -> {
                assert value != null;
                throw new UnsupportedOperationException(
                        "Cannot use get(String) on node with value type: " + value.getClass().getSimpleName());
            }
        };
    }
    // ── Field-existence predicate ──────────────────────────────────────────
    /** Returns {@code true} iff this node's value is an object containing the given field. */
    public boolean hasField(String field) {
        return value instanceof object obj && obj.has(field);
    }

    // ── Typed convenience getters (delegate to object) ─────────────────────
    public String  getString(String field)  { return requireObject().getString(field); }
    public long    getLong(String field)    { return requireObject().getLong(field); }
    public int     getInt(String field)     { return requireObject().getInt(field); }
    public double  getDouble(String field)  { return requireObject().getDouble(field); }
    public boolean getBoolean(String field) { return requireObject().getBoolean(field); }

    private object requireObject() {
        if (!(value instanceof object obj))
            throw new UnsupportedOperationException(
                "Typed getters require an object value; this node holds: "
                + (value == null ? "null" : value.getClass().getSimpleName()));
        return obj;
    }

    public value get(int index) {
        return switch (value) {
            case array a -> a.get(index);
            case tuple t -> t.get(index);
            default -> throw new UnsupportedOperationException(
                    "get(int) not supported on " + value.getClass().getSimpleName());
        };
    }
}
