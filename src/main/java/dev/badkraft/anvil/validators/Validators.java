package dev.badkraft.anvil.validators;

public class Validators {
    public static ValidationResult validateFilePath(String path) {
        // use FileStatus to validate the file path
        if (path == null) {
            return new ValidationResult(FileStatus.NULL.getCode(),
                    FileStatus.NULL.formatMessage(null));
        }
        if (path.isEmpty()) {
            return new ValidationResult(FileStatus.EMPTY.getCode(),
                    FileStatus.EMPTY.formatMessage(path));
        }
        java.nio.file.Path filePath = java.nio.file.Path.of(path);
        if (!java.nio.file.Files.exists(filePath)) {
            return new ValidationResult(FileStatus.NOT_FOUND.getCode(),
                    FileStatus.NOT_FOUND.formatMessage(path));
        }
        return new ValidationResult(FileStatus.SUCCESS.getCode(),
                FileStatus.SUCCESS.formatMessage(path));
    }
}
