package dev.badkraft.anvil.api;

import dev.badkraft.anvil.core.data.Statement;
import dev.badkraft.anvil.core.data.Value;

import java.util.List;
import java.util.NoSuchElementException;

public record AnvilRoot(
        Value.Attributes rootAttributes,
        List<Statement> statements,
        String namespace
) {


    public Value.Attributes getAttributes() {
        return  rootAttributes;
    }

    private Value valueFor(String key) {
        return statements.stream()
                .filter(s -> s.identifier().equals(key))
                .map(Statement::value)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No statement named: " + key));
    }

}
