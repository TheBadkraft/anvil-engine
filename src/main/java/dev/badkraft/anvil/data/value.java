/// src/main/java/dev/badkraft/anvil/data/null.java
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
package dev.badkraft.anvil.data;

public sealed interface value permits object, array, tuple, blob,
        value.LongValue, value.DoubleValue, value.StringValue, value.BooleanValue,
        value.NullValue {
    record LongValue(long value) implements value {
        @Override public long asLong() { return value; }
        @Override public int asInt() {
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
                throw new ArithmeticException("Long value " + value + " does not fit in int");
            return (int) value;
        }
        @Override public short asShort() {
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
                throw new ArithmeticException("Long value " + value + " does not fit in short");
            return (short) value;
        }
        @Override public byte asByte() {
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)
                throw new ArithmeticException("Long value " + value + " does not fit in byte");
            return (byte) value;
        }
        @Override public boolean isLong() { return true; }
    }

    record DoubleValue(double value) implements value {
        @Override public double asDouble() { return value; }
        @Override public float asFloat() {
            float f = (float) value;
            if (Double.isInfinite(value) || Double.isNaN(value) ||
                    (double) f != value) {
                throw new ArithmeticException("Double value " + value + " loses precision when converted to float");
            }
            return f;
        }
        @Override public boolean isDouble() { return true; }
    }

    record StringValue(String value) implements value {
        /** The stored value is always the string content — quote delimiters are stripped by the parser. */
        @Override public String asString() { return value; }
        @Override public boolean isString() { return true; }
    }

    record BooleanValue(boolean value) implements value {
        @Override public boolean asBoolean() { return value; }
        @Override public boolean isBoolean() { return true; }
    }

    record NullValue() implements value {
        @Override public boolean isNull() { return true; }
    }

    // ── Primitive accessors — throw by default ─────────────────────────────
    default long     asLong()    { throw new ClassCastException("Not a long"); }
    default double   asDouble()  {
        if (this instanceof LongValue(long value)) return (double) value;
        throw new ClassCastException("Not a double");
    }
    default String   asString()  { throw new ClassCastException("Not a string"); }
    default boolean  asBoolean() { throw new ClassCastException("Not a boolean"); }
    default int      asInt()     { throw new ClassCastException("Not an integer"); }
    default short    asShort()   { throw new ClassCastException("Not a short"); }
    default byte     asByte()    { throw new ClassCastException("Not a byte"); }
    default float    asFloat()   { throw new ClassCastException("Not a float"); }

    // ── Container accessors ────────────────────────────────────────────────
    default object  asObject()  { throw new ClassCastException("Not an object"); }
    default array   asArray()   { throw new ClassCastException("Not an array"); }
    default tuple   asTuple()   { throw new ClassCastException("Not a tuple"); }
    default blob    asBlob()    { throw new ClassCastException("Not a blob"); }

    // ── Indexer accessor ───────────────────────────────────────────────────
    default value get(int i) { throw new UnsupportedOperationException("Not indexable"); }

    // ── Object base accessors ──────────────────────────────────────────────
    default String  base()    { throw new ClassCastException("Not an object"); }
    default boolean hasBase() { return false; }

    // ── Type-check predicates (default false; each permit overrides its own) ──
    default boolean isString()  { return false; }
    default boolean isLong()    { return false; }
    default boolean isDouble()  { return false; }
    default boolean isBoolean() { return false; }
    default boolean isNull()    { return false; }
    default boolean isObject()  { return false; }
    default boolean isArray()   { return false; }
    default boolean isTuple()   { return false; }
}
