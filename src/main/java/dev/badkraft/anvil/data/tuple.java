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

import dev.badkraft.anvil.core.data.Attribute;
import dev.badkraft.anvil.core.data.Value;
import dev.badkraft.anvil.utilities.AnvilConverters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class tuple implements value {
    private final List<value> elements;
    private final LinkedHashMap<String, attribute> attributes;

    public tuple(Value.TupleValue internal) {
        this.elements = internal.elements().stream()
                .map(AnvilConverters::toValue)
                .toList();
        this.attributes = internal.attributes().stream()
                .collect(Collectors.toMap(
                        Attribute::key,
                        AnvilConverters::toAttribute,
                        (a, b) -> { throw new IllegalStateException("Duplicate attribute key in tuple: " + a.key()); },
                        LinkedHashMap::new
                ));
    }

    @Override
    public value get(int index) {
        if(index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }
        return elements.get(index);
    }
    public int size() {
        return elements.size();
    }
    public List<value> elements() {
        return List.copyOf(elements);
    }
    public List<attribute> attributes() {
        return List.copyOf(attributes.values());
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
    public tuple asTuple() {
        return this;
    }
    @Override
    public boolean isTuple() { return true; }
}
