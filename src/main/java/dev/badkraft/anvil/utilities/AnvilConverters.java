/// src/main/java/dev/badkraft/anvil/utilities/null.java
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
package dev.badkraft.anvil.utilities;

import dev.badkraft.anvil.api.node;
import dev.badkraft.anvil.core.data.Attribute;
import dev.badkraft.anvil.core.data.Statement;
import dev.badkraft.anvil.core.data.Value;
import dev.badkraft.anvil.data.*;

import java.util.List;

public final class AnvilConverters {

    private AnvilConverters() {}

    public static value toValue(Value v) {
        if (v == null) return null;
        return switch (v) {
            case Value.LongValue    l -> new value.LongValue(l.value());
            case Value.HexValue     h -> new value.LongValue(h.value());
            case Value.DoubleValue  d -> new value.DoubleValue(d.value());
            case Value.StringValue  s -> new value.StringValue(s.content());
            case Value.BareLiteral  b -> new value.StringValue(b.value());
            case Value.BooleanValue b -> new value.BooleanValue(b.value());
            case Value.NullValue    n -> new value.NullValue();
            case Value.ObjectValue  o -> new object(o);
            case Value.ArrayValue   a -> new array(a);
            case Value.TupleValue   t -> new tuple(t);
            case Value.BlobValue    b -> new blob(b);
            default -> throw new IllegalArgumentException("Unknown value type: " + v.getClass());
        };
    }

    public static attribute toAttribute(Attribute a) {
        value payload = a.value() == null ? null : toValue(a.value());
        return new attribute(a.key(), payload);
    }

    public static node toNode(Statement stmt) {
        value v = toValue(stmt.value());
        List<attribute> attrs = stmt.attributes().stream()
                .map(AnvilConverters::toAttribute)
                .toList();
        return new node(stmt.identifier(), attrs, v);
    }

}
