/// src/main/java/dev/badkraft/anvil/api/AnvilRoot.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: November 30, 2025
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

import java.util.*;
import java.util.stream.Collectors;

public final class root {
    private IResolver resolver = IResolver.EMPTY;
    private final LinkedHashMap<String, node> nodes;
    private final LinkedHashMap<String, attribute> attributes;

    public root(List<node> nodes, List<attribute> attributes) {
        this.nodes = nodes.stream()
                .collect(Collectors.toMap(
                        node::identifier,
                        n -> n,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        this.attributes = attributes.stream()
                .collect(Collectors.toMap(
                        attribute::key,
                        a -> a,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    //  [0.1.7] new API stubs
    public List<attribute> attributes() {
        // return immutable list
        return List.copyOf(attributes.values());
    }
    public List<node> nodes() {
        return List.copyOf(nodes.values());
    }
    public node node(String key) {
        if (!nodes.containsKey(key)) {
            throw new NoSuchElementException("No node found with key: " + key);
        }
        return nodes.get(key);
    }
    public attribute attribute(String key) {
        if(!attributes.containsKey(key)) {
            throw new NoSuchElementException("No attribute found with key: " + key);
        }
        return attributes.get(key);
    }
    public value get(String key) {
        return node(key).value();
    }
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    /** Returns {@code true} iff a node with the given key exists in this root. */
    public boolean hasNode(String key) {
        return nodes.containsKey(key);
    }
    public object resolveBase(String identifier) {
        return resolver.resolveBase(identifier);
    }

    public void setResolver(IResolver resolver) {
        this.resolver = resolver;
    }
    public IResolver resolver() { return resolver; }
    // Package-private — only for Resolver
    public LinkedHashMap<String, node> nodesById() {
        return nodes;
    }
}
