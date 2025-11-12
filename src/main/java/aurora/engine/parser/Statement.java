package aurora.engine.parser;

/*
    A Statement is a top-level construct in an Aurora document.
    Currently, only Assignment statements are supported.
 */
public sealed interface Statement permits Assignment {
}
