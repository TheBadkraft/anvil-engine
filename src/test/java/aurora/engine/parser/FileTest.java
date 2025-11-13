// src/test/java/aurora/engine/parser/FileTest.java
package aurora.engine.parser;

import java.nio.file.*;

import aurora.engine.validators.ValidationResult;
import aurora.engine.validators.Validators;


public class FileTest {
    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : null;

        // Validate file path
        ValidationResult validationResult =
                Validators.validateFilePath(filePath);
        Path path;

        switch (validationResult.getCode()) {
            case 0:
                System.out.println(validationResult.getMessage());
                assert filePath != null;    // we know it's not null here
                path = Paths.get(filePath);

                break;
            case 1:
            case 2:
            case 3:
                System.err.println("File validation failed: " + validationResult.getMessage());
                return;
            default:
                System.err.println("Unknown validation result code: " + validationResult.getCode());
                return;
        }

        ParseResult<?> result = AuroraParser.parse(path); // lexer-less
        var document = (AuroraDocument)result.result();
        if (document == null) {
            // print errors if any
            if (!result.errors().isEmpty()) {
                System.out.printf("FAILED — %d error(s)%n", result.errors().size());
                result.errors().forEach(e -> System.err.printf(" [%d:%d] %s%n", e.line(),
                    e.col(), e.message()));
            }
            else {
                System.out.println("Document failed to parse.");
            }

            return;
        }
        if (document.isEmpty()) {
            System.out.println("Document is empty.");
            return;
        }
        if (document.isParsed()) {
            System.out.println("Document is parsed.");
            if(document.hasNoDialect()) {
                System.out.println("Document dialect could not be determined.");
            }
            else {
                System.out.println("Document dialect: " + document.getDialect());
            }
            var fields = document.fields();
            if (!fields.isEmpty()){
                // pretty print fields
                System.out.println("Fields found:");
                for (String fld : fields) {
                    System.out.println(" - " + fld);
                }
            }
            if (document.hasStatements()){
                // pretty print statements
                System.out.println("Statements found:");
                for (Statement stmt : document.statements()) {
                    System.out.println(" - " + stmt);
                }
            }
        } else {
            System.out.println("Document did not complete parsing.");
        }
        if (!result.errors().isEmpty()) {
            System.out.printf("FAILED — %d error(s)%n", result.errors().size());
            result.errors().forEach(e -> System.err.printf(" [%d:%d] %s%n", e.line(),
                e.col(), e.message()));
            return;
        }

        System.out.println("PASS");
        System.out.println(result.result());
    }
}
