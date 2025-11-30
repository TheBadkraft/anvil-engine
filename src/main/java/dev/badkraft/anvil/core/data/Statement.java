package dev.badkraft.anvil.core.data;

import java.util.List;

/*
    A Statement is a top-level construct in an Aurora document.
    Currently, only Assignment statements are supported.
 */
public sealed interface Statement permits Assignment {
    /** Immutable attribues attached to this statement. */
    List<Attribute> attributes();
    String identifier();
    Value value();
    String base();
}
