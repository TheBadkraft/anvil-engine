package dev.badkraft.anvil.utilities;

import dev.badkraft.anvil.validators.ValidationResult;
import dev.badkraft.anvil.validators.Validators;

import java.io.IOException;
import java.nio.file.Path;

public class Utils {

    // Get file extension from Path
    public static String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ""; // No extension found
        }
        return fileName.substring(lastDotIndex + 1);
    }

    public static String loadFile(Path path) throws IOException {
        ValidationResult result = Validators.validateFilePath(path.toString());
        if(result.getCode() == 0) {
            try {
                return java.nio.file.Files.readString(path);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read file: " + path, e);
            }
        } else {
            throw new IOException("Invalid file path: " + path.toString());
        }
    }

    public static String createNamespaceFromPath(Path path) {
        // Simple namespace creation based on file path (parent) without extension
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        String baseName = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
        return baseName + path.getParent().getFileName() + "." + baseName + "_" + genHash();
    }

    public static String createNamespace() {
        return "ns_" + genHash();
    }

    private  static String genHash() {
        // generate a simple hesadcimal (byte) hash, 9 digiets long
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            int digit = (int) (Math.random() * 16);
            sb.append(Integer.toHexString(digit));
        }
        return sb.toString();
    }
}
