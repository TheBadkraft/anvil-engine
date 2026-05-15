/// src/main/java/dev/badkraft/anvil/core/data/Dialect.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: November 14, 2025
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
package dev.badkraft.anvil.core.data;

public enum Dialect {
    ASL(0, "asl"),
    AML(1, "aml"),
    NONE(-1, "nil"),;

    private final int code;
    private final String name;

    Dialect(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    // to string implementation
    @Override
    public String toString() {
        return name.toUpperCase();
    }

    public static Dialect fromFileExtension(String fileExtension) {
        return switch (fileExtension) {
            case "asl" -> ASL;
            case "aml", "anvl" -> AML;  // .anvl is the canonical minecraft.ic extension for AML
            // let's return NONE for unsupported extensions, let the caller handle it
            default -> NONE;
        };
    }

    public static Dialect fromShebang(String token) {
        return switch (token) {
            case "#!asl" -> ASL;
            case "#!aml" -> AML;
            default -> throw new IllegalArgumentException("Unsupported shebang token: " + token);
        };
    }
}
